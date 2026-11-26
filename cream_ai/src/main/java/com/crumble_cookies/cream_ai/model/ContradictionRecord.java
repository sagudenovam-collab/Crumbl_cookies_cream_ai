package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Запись механизма "Contradiction Detective". */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContradictionRecord {
    private String id;
    private List<String> requirementIds;
    /** requirementId -> точная цитата из этого требования */
    private Map<String, String> quotes;
    private String explanation;
    private String possibleConsequence;
    private String resolutionSuggestion;
    private EvidenceStatus status;
    private double confidence;
}
