package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.llm.JsonExtractionUtil;
import com.crumble_cookies.cream_ai.llm.LLMException;
import com.crumble_cookies.cream_ai.llm.LLMProvider;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Priority;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.Severity;
import com.crumble_cookies.cream_ai.model.SourceLocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Анализирует каждое требование отдельно по критериям ISO/IEC/IEEE 29148 и IEEE 830. */
@Component
public class IndividualRequirementAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(IndividualRequirementAnalyzer.class);
    private static final int BATCH_SIZE = 8;

    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IndividualRequirementAnalyzer(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public List<Finding> analyze(List<Requirement> requirements) throws LLMException {
        List<Finding> findings = new ArrayList<>();
        Map<String, Requirement> byId = requirements.stream()
                .collect(java.util.stream.Collectors.toMap(Requirement::getInternalId, r -> r));

        List<List<Requirement>> batches = partition(requirements, BATCH_SIZE);
        int batchNo = 0;
        for (List<Requirement> batch : batches) {
            batchNo++;
            log.info("Индивидуальный анализ: батч {}/{} ({} требований)", batchNo, batches.size(), batch.size());
            String userPrompt = buildUserPrompt(batch);
            String raw = llmProvider.complete(AnalysisPrompts.INDIVIDUAL_SYSTEM_PROMPT, userPrompt);
            String json = JsonExtractionUtil.extractJson(raw);
            List<IndividualRequirementResultDto> results = parseResults(json);

            AtomicInteger counter = new AtomicInteger(1);
            for (IndividualRequirementResultDto dto : results) {
                Requirement requirement = byId.get(dto.requirementId());
                if (requirement == null) {
                    continue; // модель сослалась на несуществующий ID — игнорируем, не придумываем связь
                }
                if (dto.findings() == null) {
                    continue;
                }
                for (IndividualFindingDto f : dto.findings()) {
                    findings.add(toFinding(requirement, f, counter.getAndIncrement()));
                }
            }
        }
        return findings;
    }

    private Finding toFinding(Requirement requirement, IndividualFindingDto dto, int seq) {
        SourceLocation location = requirement.getLocation();
        return Finding.builder()
                .id("IND-" + requirement.getInternalId() + "-" + seq)
                .relatedRequirementIds(List.of(requirement.getInternalId()))
                .criterion(dto.criterion())
                .quote(dto.quote())
                .location(location)
                .explanation(dto.explanation())
                .severity(parseSeverity(dto.severity()))
                .severityReason(dto.severityReason())
                .confidence(clamp(dto.confidence()))
                .recommendation(dto.recommendation())
                .improvedWording(dto.improvedWording())
                .scope("INDIVIDUAL")
                .build();
    }

    private Severity parseSeverity(String s) {
        if (s == null) return Severity.LOW;
        try {
            return Severity.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Severity.LOW;
        }
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private List<IndividualRequirementResultDto> parseResults(String json) throws LLMException {
        try {
            var node = objectMapper.readTree(json);
            var resultsNode = node.has("results") ? node.get("results") : node;
            return objectMapper.convertValue(resultsNode,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, IndividualRequirementResultDto.class));
        } catch (Exception e) {
            throw new LLMException(LLMException.Kind.INVALID_RESPONSE,
                    "Не удалось разобрать JSON индивидуального анализа: " + e.getMessage(), e);
        }
    }

    private String buildUserPrompt(List<Requirement> batch) {
        StringBuilder sb = new StringBuilder();
        for (Requirement r : batch) {
            sb.append("ID: ").append(r.getInternalId()).append('\n');
            sb.append("Текст: ").append(r.getOriginalText()).append("\n\n");
        }
        return sb.toString();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        if (result.isEmpty()) {
            result.add(List.of());
        }
        return result;
    }
}
