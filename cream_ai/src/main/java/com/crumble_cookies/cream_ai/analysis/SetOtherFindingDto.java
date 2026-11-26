package com.crumble_cookies.cream_ai.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetOtherFindingDto(
        List<String> requirementIds,
        String criterion,
        String quote,
        String explanation,
        String severity,
        String severityReason,
        double confidence,
        String recommendation
) {
}
