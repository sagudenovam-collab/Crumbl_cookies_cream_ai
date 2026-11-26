package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Прозрачная расшифровка Requirement Health Score (0-100).
 * Это собственная методика продукта, а не официальная оценка ISO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreBreakdown {

    private int overallScore;

    /** Баллы по отдельным критериям ISO/IEEE (0-100 по каждому). */
    private Map<String, Integer> byCriterion;

    /** Распределение дефектов по серьёзности (количество). */
    private Map<Severity, Long> severityDistribution;

    /** Штраф за системные проблемы набора требований (0-100, вычитается). */
    private int setLevelPenalty;

    /** Коэффициент доказанности замечаний (0.0 - 1.0). */
    private double evidenceConfidenceFactor;

    /** Человеко-читаемые пояснения, из чего складывается итоговый балл. */
    private List<String> explanationNotes;
}
