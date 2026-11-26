package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Полный результат анализа документа требований. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    private AnalysisPassport passport;
    private ExecutiveSummary executiveSummary;
    private HealthScoreBreakdown healthScore;

    private List<Requirement> requirements;

    /** Замечания по отдельным требованиям. */
    private List<Finding> individualFindings;

    /** Замечания уровня набора (не привязанные к одному требованию). */
    private List<Finding> setLevelFindings;

    private List<ContradictionRecord> contradictions;
    private List<DuplicateRecord> duplicates;
    private List<TraceabilityRow> traceabilityMatrix;

    private Verdict verdict;
    private String verdictExplanation;

    /** Требования/замечания, помеченные для проверки человеком. */
    private List<String> itemsNeedingHumanReview;
}
