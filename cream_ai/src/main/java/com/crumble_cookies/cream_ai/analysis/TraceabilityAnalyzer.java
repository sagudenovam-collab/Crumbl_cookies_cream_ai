package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.DuplicateRecord;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.TraceabilityRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Строит матрицу прослеживаемости. Связи между требованиями берутся только
 * из реально обнаруженных противоречий/дубликатов/системных замечаний —
 * если связей не найдено, они не придумываются (пустой список).
 */
@Component
public class TraceabilityAnalyzer {

    public List<TraceabilityRow> build(List<Requirement> requirements,
                                        List<Finding> individualFindings,
                                        List<Finding> setFindings,
                                        List<ContradictionRecord> contradictions,
                                        List<DuplicateRecord> duplicates) {
        List<TraceabilityRow> rows = new ArrayList<>();
        for (Requirement requirement : requirements) {
            String id = requirement.getInternalId();

            Set<String> related = new LinkedHashSet<>();
            List<String> findingIds = new ArrayList<>();
            Set<String> criteria = new LinkedHashSet<>();
            Set<String> recommendations = new LinkedHashSet<>();

            for (Finding f : individualFindings) {
                if (f.getRelatedRequirementIds() != null && f.getRelatedRequirementIds().contains(id)) {
                    findingIds.add(f.getId());
                    if (f.getCriterion() != null) criteria.add(f.getCriterion());
                    if (f.getRecommendation() != null) recommendations.add(f.getRecommendation());
                }
            }
            for (Finding f : setFindings) {
                if (f.getRelatedRequirementIds() != null && f.getRelatedRequirementIds().contains(id)) {
                    findingIds.add(f.getId());
                    if (f.getCriterion() != null) criteria.add(f.getCriterion());
                    if (f.getRecommendation() != null) recommendations.add(f.getRecommendation());
                    f.getRelatedRequirementIds().stream().filter(x -> !x.equals(id)).forEach(related::add);
                }
            }
            for (ContradictionRecord c : contradictions) {
                if (c.getRequirementIds().contains(id)) {
                    findingIds.add(c.getId());
                    criteria.add("Согласованность (противоречие)");
                    c.getRequirementIds().stream().filter(x -> !x.equals(id)).forEach(related::add);
                    if (c.getResolutionSuggestion() != null) recommendations.add(c.getResolutionSuggestion());
                }
            }
            for (DuplicateRecord d : duplicates) {
                if (d.getRequirementIds().contains(id)) {
                    findingIds.add(d.getId());
                    criteria.add("Необходимость (дублирование)");
                    d.getRequirementIds().stream().filter(x -> !x.equals(id)).forEach(related::add);
                }
            }

            rows.add(TraceabilityRow.builder()
                    .internalId(id)
                    .originalId(requirement.getOriginalId())
                    .relatedRequirementIds(new ArrayList<>(related))
                    .findingIds(findingIds)
                    .violatedCriteria(new ArrayList<>(criteria))
                    .recommendations(new ArrayList<>(recommendations))
                    .build());
        }
        return rows;
    }
}
