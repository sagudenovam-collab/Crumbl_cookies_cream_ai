package com.crumble_cookies.cream_ai.extraction;

import com.crumble_cookies.cream_ai.llm.JsonExtractionUtil;
import com.crumble_cookies.cream_ai.llm.LLMException;
import com.crumble_cookies.cream_ai.llm.LLMProvider;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.RequirementType;
import com.crumble_cookies.cream_ai.model.SourceLocation;
import com.crumble_cookies.cream_ai.parser.DocumentSegment;
import com.crumble_cookies.cream_ai.parser.ParsedDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Извлекает отдельные требования из распарсенного документа с помощью LLM.
 * Чтобы модель не придумывала координаты, каждому фрагменту документа
 * присваивается индекс [S{n}], и модель ссылается на эти индексы — реальные
 * страница/строка берутся из исходных сегментов, а не из ответа модели.
 */
@Component
public class RequirementExtractor {

    private static final Logger log = LoggerFactory.getLogger(RequirementExtractor.class);

    /** Максимальный суммарный размер текста сегментов в одном чанке (символы). */
    private static final int CHUNK_CHAR_BUDGET = 9000;

    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequirementExtractor(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public List<Requirement> extract(ParsedDocument document) throws LLMException {
        List<Requirement> result = new ArrayList<>();
        List<List<Integer>> chunks = chunkSegments(document.segments());

        int chunkNo = 0;
        for (List<Integer> chunkIndices : chunks) {
            chunkNo++;
            log.info("Извлечение требований: чанк {}/{} ({} фрагментов)", chunkNo, chunks.size(), chunkIndices.size());
            String userPrompt = buildUserPrompt(document, chunkIndices);
            String rawResponse = llmProvider.complete(ExtractionPrompts.SYSTEM_PROMPT, userPrompt);
            String json = JsonExtractionUtil.extractJson(rawResponse);
            List<ExtractionCandidate> candidates = parseCandidates(json);

            for (ExtractionCandidate candidate : candidates) {
                if (!candidate.isActualRequirement()) {
                    continue;
                }
                result.add(toRequirement(document, candidate));
            }
        }
        return result;
    }

    private List<ExtractionCandidate> parseCandidates(String json) throws LLMException {
        try {
            if (json.trim().startsWith("[")) {
                return List.of(objectMapper.readValue(json, ExtractionCandidate[].class));
            }
            // модель иногда оборачивает массив в объект {"requirements": [...]}
            var node = objectMapper.readTree(json);
            if (node.has("requirements")) {
                return objectMapper.convertValue(node.get("requirements"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ExtractionCandidate.class));
            }
            return List.of(objectMapper.treeToValue(node, ExtractionCandidate.class));
        } catch (Exception e) {
            throw new LLMException(LLMException.Kind.INVALID_RESPONSE,
                    "Не удалось разобрать JSON с извлечёнными требованиями: " + e.getMessage(), e);
        }
    }

    private Requirement toRequirement(ParsedDocument document, ExtractionCandidate candidate) {
        SourceLocation location = resolveLocation(document, candidate.segmentRefs());
        return Requirement.builder()
                .originalId(blankToNull(candidate.originalId()))
                .originalText(candidate.text())
                .summary(candidate.summary())
                .source(document.fileName())
                .location(location)
                .type(parseType(candidate.type()))
                .needsHumanReview(candidate.ambiguous())
                .humanReviewReason(candidate.ambiguousReason())
                .build();
    }

    private SourceLocation resolveLocation(ParsedDocument document, List<Integer> segmentRefs) {
        if (segmentRefs == null || segmentRefs.isEmpty()) {
            return SourceLocation.unknown(document.fileName());
        }
        int idx = segmentRefs.get(0);
        if (idx < 0 || idx >= document.segments().size()) {
            return SourceLocation.unknown(document.fileName());
        }
        DocumentSegment segment = document.segments().get(idx);
        return new SourceLocation(document.fileName(), segment.page(), segment.line());
    }

    private RequirementType parseType(String type) {
        if (type == null) {
            return RequirementType.UNKNOWN;
        }
        try {
            return RequirementType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RequirementType.UNKNOWN;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Разбивает сегменты документа на чанки по бюджету символов, чтобы помещаться в контекст LLM. */
    private List<List<Integer>> chunkSegments(List<DocumentSegment> segments) {
        List<List<Integer>> chunks = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int currentSize = 0;
        for (int i = 0; i < segments.size(); i++) {
            int len = segments.get(i).text().length();
            if (currentSize + len > CHUNK_CHAR_BUDGET && !current.isEmpty()) {
                chunks.add(current);
                current = new ArrayList<>();
                currentSize = 0;
            }
            current.add(i);
            currentSize += len;
        }
        if (!current.isEmpty()) {
            chunks.add(current);
        }
        if (chunks.isEmpty()) {
            chunks.add(List.of());
        }
        return chunks;
    }

    private String buildUserPrompt(ParsedDocument document, List<Integer> indices) {
        StringBuilder sb = new StringBuilder();
        sb.append("Документ: ").append(document.fileName()).append("\n\n");
        sb.append("Фрагменты документа (используй индекс [S{n}] в поле segmentRefs, НЕ придумывай номера страниц/строк):\n\n");
        for (int idx : indices) {
            DocumentSegment seg = document.segments().get(idx);
            sb.append("[S").append(idx).append("] ").append(seg.text()).append("\n");
        }
        return sb.toString();
    }
}
