package com.crumble_cookies.cream_ai.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetAnalysisResponseDto(
        List<SetContradictionDto> contradictions,
        List<SetDuplicateDto> duplicates,
        List<SetOtherFindingDto> other
) {
}
