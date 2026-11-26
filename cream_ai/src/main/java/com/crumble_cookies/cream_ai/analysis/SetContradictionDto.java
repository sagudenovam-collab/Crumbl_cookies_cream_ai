package com.crumble_cookies.cream_ai.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetContradictionDto(
        List<String> requirementIds,
        Map<String, String> quotes,
        String explanation,
        String possibleConsequence,
        String resolutionSuggestion,
        String status,
        double confidence
) {
}
