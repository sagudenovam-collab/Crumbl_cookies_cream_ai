package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.DuplicateRecord;
import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.HealthScoreBreakdown;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Рассчитывает Requirement Health Score (0-100) — собственную прозрачную
 * методику продукта (НЕ официальную оценку ISO). Балл складывается из:
 * - индивидуального качества требований (по критериям ISO/IEEE);
 * - серьёзности найденных дефектов;
 * - системных проблем набора (противоречия/дубликаты);
 * - доказанности замечаний (чем больше неподтверждённых замечаний, тем
 *   мягче штраф — мы не создаём ложное ощущение математической точности).
 */
@Component
public class RequirementScorer {

    private static final Map<Severity, Integer> SEVERITY_WEIGHT = Map.of(
            Severity.CRITICAL, 18,
            Severity.HIGH, 10,
            Severity.MEDIUM, 5,
            Severity.LOW, 2
    );

    public HealthScoreBreakdown score(List<Requirement> requirements,
                                       List<Finding> individualFindings,
                                       List<Finding> setFindings,
                                       List<ContradictionRecord> contradictions,
                                       List<DuplicateRecord> duplicates) {
        List<String> notes = new ArrayList<>();
        int requirementCount = Math.max(1, requirements.size());

        List<Finding> allFindings = new ArrayList<>();
        allFindings.addAll(individualFindings);
        allFindings.addAll(setFindings);

        Map<String, Integer> byCriterion = computeByCriterion(individualFindings, requirementCount, notes);
        Map<Severity, Long> severityDistribution = computeSeverityDistribution(allFindings);

        double individualAvg = byCriterion.isEmpty()
                ? 100.0
                : byCriterion.values().stream().mapToInt(Integer::intValue).average().orElse(100.0);

        int setLevelPenalty = computeSetLevelPenalty(contradictions, duplicates, requirementCount, notes);

        double evidenceConfidenceFactor = computeEvidenceConfidenceFactor(allFindings, contradictions, duplicates);

        double raw = individualAvg - setLevelPenalty;
        raw = raw * (0.75 + 0.25 * evidenceConfidenceFactor);
        int overallScore = (int) Math.round(clamp(raw, 0, 100));

        notes.add("Индивидуальное качество требований (среднее по критериям ISO/IEEE): "
                + Math.round(individualAvg) + "/100.");
        notes.add("Штраф за системные проблемы набора (противоречия/дубликаты): -" + setLevelPenalty + ".");
        notes.add("Коэффициент доказанности замечаний: " + String.format("%.2f", evidenceConfidenceFactor)
                + " (учитывает долю подтверждённых против предполагаемых замечаний).");
        notes.add("Это собственная прозрачная методика продукта, а не официальная оценка ISO.");

        return HealthScoreBreakdown.builder()
                .overallScore(overallScore)
                .byCriterion(byCriterion)
                .severityDistribution(severityDistribution)
                .setLevelPenalty(setLevelPenalty)
                .evidenceConfidenceFactor(round2(evidenceConfidenceFactor))
                .explanationNotes(notes)
                .build();
    }

    private Map<String, Integer> computeByCriterion(List<Finding> individualFindings, int requirementCount, List<String> notes) {
        Map<String, Double> penaltyByCriterion = new LinkedHashMap<>();
        for (Finding f : individualFindings) {
            if (f.getCriterion() == null) continue;
            double weight = SEVERITY_WEIGHT.getOrDefault(f.getSeverity(), 5) * evidenceWeight(f.getStatus());
            penaltyByCriterion.merge(f.getCriterion(), weight, Double::sum);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : penaltyByCriterion.entrySet()) {
            double normalizedPenalty = (e.getValue() / requirementCount) * 10.0;
            int score = (int) Math.round(clamp(100 - normalizedPenalty, 0, 100));
            result.put(e.getKey(), score);
        }
        return result;
    }

    private Map<Severity, Long> computeSeverityDistribution(List<Finding> findings) {
        Map<Severity, Long> dist = new LinkedHashMap<>();
        for (Severity s : Severity.values()) {
            dist.put(s, 0L);
        }
        for (Finding f : findings) {
            if (f.getStatus() == EvidenceStatus.CONFIRMED_ISSUE || f.getStatus() == EvidenceStatus.POTENTIAL_ISSUE) {
                dist.merge(f.getSeverity(), 1L, Long::sum);
            }
        }
        return dist;
    }

    private int computeSetLevelPenalty(List<ContradictionRecord> contradictions, List<DuplicateRecord> duplicates,
                                        int requirementCount, List<String> notes) {
        double penalty = 0;
        for (ContradictionRecord c : contradictions) {
            penalty += 9 * evidenceWeight(c.getStatus());
        }
        for (DuplicateRecord d : duplicates) {
            penalty += 4 * evidenceWeight(d.getStatus());
        }
        double normalized = (penalty / requirementCount) * 10.0;
        return (int) Math.round(clamp(normalized, 0, 40));
    }

    private double computeEvidenceConfidenceFactor(List<Finding> findings, List<ContradictionRecord> contradictions,
                                                     List<DuplicateRecord> duplicates) {
        List<Double> weights = new ArrayList<>();
        for (Finding f : findings) {
            weights.add(evidenceWeight(f.getStatus()));
        }
        for (ContradictionRecord c : contradictions) {
            weights.add(evidenceWeight(c.getStatus()));
        }
        for (DuplicateRecord d : duplicates) {
            weights.add(evidenceWeight(d.getStatus()));
        }
        if (weights.isEmpty()) {
            return 1.0;
        }
        return weights.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
    }

    private double evidenceWeight(EvidenceStatus status) {
        if (status == null) return 0.3;
        return switch (status) {
            case CONFIRMED_ISSUE -> 1.0;
            case POTENTIAL_ISSUE -> 0.5;
            case NEEDS_HUMAN_REVIEW -> 0.25;
        };
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
