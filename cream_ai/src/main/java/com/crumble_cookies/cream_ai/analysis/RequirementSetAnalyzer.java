package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.llm.JsonExtractionUtil;
import com.crumble_cookies.cream_ai.llm.LLMException;
import com.crumble_cookies.cream_ai.llm.LLMProvider;
import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.DuplicateRecord;
import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Анализирует ВЕСЬ набор требований в совокупности: противоречия, дубликаты,
 * пробелы и прочие системные проблемы. Для больших документов использует
 * разбиение на чанки с перекрытием и дополнительный "digest"-проход по всему
 * набору, чтобы не ограничиваться требованиями из одного фрагмента.
 */
@Component
public class RequirementSetAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RequirementSetAnalyzer.class);

    /** Количество требований в одном чанке для детального анализа. */
    private static final int CHUNK_SIZE = 25;
    /** Перекрытие между соседними чанками, чтобы ловить противоречия на границах. */
    private static final int CHUNK_OVERLAP = 5;

    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequirementSetAnalyzer(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public record SetAnalysisOutput(
            List<ContradictionRecord> contradictions,
            List<DuplicateRecord> duplicates,
            List<Finding> otherFindings
    ) {
    }

    public SetAnalysisOutput analyze(List<Requirement> requirements) throws LLMException {
        if (requirements.isEmpty()) {
            return new SetAnalysisOutput(List.of(), List.of(), List.of());
        }

        Map<String, Requirement> byId = new LinkedHashMap<>();
        for (Requirement r : requirements) {
            byId.put(r.getInternalId(), r);
        }

        List<List<Requirement>> chunks = chunkWithOverlap(requirements, CHUNK_SIZE, CHUNK_OVERLAP);

        List<ContradictionRecord> contradictions = new ArrayList<>();
        List<DuplicateRecord> duplicates = new ArrayList<>();
        List<Finding> others = new ArrayList<>();
        Set<Set<String>> seenContradictionKeys = new LinkedHashSet<>();
        Set<Set<String>> seenDuplicateKeys = new LinkedHashSet<>();

        AtomicInteger contradictionSeq = new AtomicInteger(1);
        AtomicInteger duplicateSeq = new AtomicInteger(1);
        AtomicInteger otherSeq = new AtomicInteger(1);

        int chunkNo = 0;
        for (List<Requirement> chunk : chunks) {
            chunkNo++;
            log.info("Анализ набора требований: чанк {}/{} ({} требований)", chunkNo, chunks.size(), chunk.size());
            SetAnalysisResponseDto dto = callSetAnalysis(chunk);
            mergeContradictions(dto, byId, contradictions, seenContradictionKeys, contradictionSeq);
            mergeDuplicates(dto, byId, duplicates, seenDuplicateKeys, duplicateSeq);
            mergeOther(dto, byId, others, otherSeq);
        }

        if (chunks.size() > 1) {
            runDigestPass(requirements, byId, contradictions, duplicates,
                    seenContradictionKeys, seenDuplicateKeys, contradictionSeq, duplicateSeq);
        }

        return new SetAnalysisOutput(contradictions, duplicates, others);
    }

    private void runDigestPass(List<Requirement> all, Map<String, Requirement> byId,
                                List<ContradictionRecord> contradictions, List<DuplicateRecord> duplicates,
                                Set<Set<String>> seenContradictionKeys, Set<Set<String>> seenDuplicateKeys,
                                AtomicInteger contradictionSeq, AtomicInteger duplicateSeq) throws LLMException {
        log.info("Анализ набора требований: сквозной digest-проход по {} требованиям", all.size());
        StringBuilder sb = new StringBuilder();
        for (Requirement r : all) {
            sb.append(r.getInternalId()).append(" [").append(r.getType()).append("] ")
                    .append(r.getSummary() != null ? r.getSummary() : truncate(r.getOriginalText()))
                    .append('\n');
        }
        String raw;
        try {
            raw = llmProvider.complete(AnalysisPrompts.DIGEST_SYSTEM_PROMPT, sb.toString());
        } catch (LLMException e) {
            log.warn("Digest-проход не удался, продолжаем без него: {}", e.getMessage());
            return;
        }
        String json;
        List<DigestPairDto> pairs;
        try {
            json = JsonExtractionUtil.extractJson(raw);
            var node = objectMapper.readTree(json);
            var pairsNode = node.has("suspectedPairs") ? node.get("suspectedPairs") : node;
            pairs = objectMapper.convertValue(pairsNode,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DigestPairDto.class));
        } catch (Exception e) {
            log.warn("Не удалось разобрать ответ digest-прохода, пропускаем: {}", e.getMessage());
            return;
        }

        for (DigestPairDto pair : pairs) {
            if (pair.requirementIds() == null || pair.requirementIds().size() < 2) {
                continue;
            }
            Set<String> key = new LinkedHashSet<>(pair.requirementIds());
            if (seenContradictionKeys.contains(key) || seenDuplicateKeys.contains(key)) {
                continue; // уже проверено детальным чанк-анализом
            }
            List<Requirement> focusGroup = new ArrayList<>();
            for (String id : pair.requirementIds()) {
                Requirement r = byId.get(id);
                if (r != null) {
                    focusGroup.add(r);
                }
            }
            if (focusGroup.size() < 2) {
                continue;
            }
            SetAnalysisResponseDto focusResult = callSetAnalysis(focusGroup);
            mergeContradictions(focusResult, byId, contradictions, seenContradictionKeys, contradictionSeq);
            mergeDuplicates(focusResult, byId, duplicates, seenDuplicateKeys, duplicateSeq);
        }
    }

    private SetAnalysisResponseDto callSetAnalysis(List<Requirement> chunk) throws LLMException {
        String userPrompt = buildUserPrompt(chunk);
        String raw = llmProvider.complete(AnalysisPrompts.SET_SYSTEM_PROMPT, userPrompt);
        String json = JsonExtractionUtil.extractJson(raw);
        try {
            return objectMapper.readValue(json, SetAnalysisResponseDto.class);
        } catch (Exception e) {
            throw new LLMException(LLMException.Kind.INVALID_RESPONSE,
                    "Не удалось разобрать JSON анализа набора требований: " + e.getMessage(), e);
        }
    }

    private void mergeContradictions(SetAnalysisResponseDto dto, Map<String, Requirement> byId,
                                      List<ContradictionRecord> out, Set<Set<String>> seenKeys, AtomicInteger seq) {
        if (dto.contradictions() == null) return;
        for (SetContradictionDto c : dto.contradictions()) {
            List<String> validIds = filterValidIds(c.requirementIds(), byId);
            if (validIds.size() < 2) continue;
            Set<String> key = new LinkedHashSet<>(validIds);
            if (!seenKeys.add(key)) continue;
            out.add(ContradictionRecord.builder()
                    .id("CTR-" + seq.getAndIncrement())
                    .requirementIds(validIds)
                    .quotes(c.quotes() != null ? c.quotes() : Map.of())
                    .explanation(c.explanation())
                    .possibleConsequence(c.possibleConsequence())
                    .resolutionSuggestion(c.resolutionSuggestion())
                    .status(parseStatus(c.status()))
                    .confidence(clamp(c.confidence()))
                    .build());
        }
    }

    private void mergeDuplicates(SetAnalysisResponseDto dto, Map<String, Requirement> byId,
                                  List<DuplicateRecord> out, Set<Set<String>> seenKeys, AtomicInteger seq) {
        if (dto.duplicates() == null) return;
        for (SetDuplicateDto d : dto.duplicates()) {
            List<String> validIds = filterValidIds(d.requirementIds(), byId);
            if (validIds.size() < 2) continue;
            Set<String> key = new LinkedHashSet<>(validIds);
            if (!seenKeys.add(key)) continue;
            out.add(DuplicateRecord.builder()
                    .id("DUP-" + seq.getAndIncrement())
                    .requirementIds(validIds)
                    .explanation(d.explanation())
                    .status(parseStatus(d.status()))
                    .confidence(clamp(d.confidence()))
                    .build());
        }
    }

    private void mergeOther(SetAnalysisResponseDto dto, Map<String, Requirement> byId,
                             List<Finding> out, AtomicInteger seq) {
        if (dto.other() == null) return;
        for (SetOtherFindingDto o : dto.other()) {
            List<String> validIds = filterValidIds(o.requirementIds(), byId);
            if (validIds.isEmpty()) continue;
            Requirement first = byId.get(validIds.get(0));
            out.add(Finding.builder()
                    .id("SET-" + seq.getAndIncrement())
                    .relatedRequirementIds(validIds)
                    .criterion(o.criterion())
                    .quote(o.quote())
                    .location(first != null ? first.getLocation() : null)
                    .explanation(o.explanation())
                    .severity(parseSeverity(o.severity()))
                    .severityReason(o.severityReason())
                    .confidence(clamp(o.confidence()))
                    .recommendation(o.recommendation())
                    .scope("SET")
                    .build());
        }
    }

    private List<String> filterValidIds(List<String> ids, Map<String, Requirement> byId) {
        if (ids == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String id : ids) {
            if (byId.containsKey(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private EvidenceStatus parseStatus(String s) {
        if (s == null) return EvidenceStatus.NEEDS_HUMAN_REVIEW;
        try {
            return EvidenceStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EvidenceStatus.NEEDS_HUMAN_REVIEW;
        }
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

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }

    private String buildUserPrompt(List<Requirement> chunk) {
        StringBuilder sb = new StringBuilder();
        for (Requirement r : chunk) {
            sb.append("ID: ").append(r.getInternalId()).append('\n');
            sb.append("Текст: ").append(r.getOriginalText()).append("\n\n");
        }
        return sb.toString();
    }

    private List<List<Requirement>> chunkWithOverlap(List<Requirement> list, int size, int overlap) {
        List<List<Requirement>> result = new ArrayList<>();
        if (list.size() <= size) {
            result.add(list);
            return result;
        }
        int step = Math.max(1, size - overlap);
        for (int start = 0; start < list.size(); start += step) {
            int end = Math.min(start + size, list.size());
            result.add(list.subList(start, end));
            if (end == list.size()) {
                break;
            }
        }
        return result;
    }
}
