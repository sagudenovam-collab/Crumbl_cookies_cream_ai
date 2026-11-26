package com.crumble_cookies.cream_ai.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Сырой кандидат в требования, возвращённый LLM на этапе извлечения. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractionCandidate(
        String originalId,
        String text,
        String summary,
        String type,
        List<Integer> segmentRefs,
        boolean ambiguous,
        String ambiguousReason,
        boolean isActualRequirement
) {
}
