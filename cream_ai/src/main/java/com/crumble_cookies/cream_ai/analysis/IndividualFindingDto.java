package com.crumble_cookies.cream_ai.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IndividualFindingDto(
        String criterion,
        String quote,
        String explanation,
        String severity,
        String severityReason,
        double confidence,
        String recommendation,
        String improvedWording
) {
}
