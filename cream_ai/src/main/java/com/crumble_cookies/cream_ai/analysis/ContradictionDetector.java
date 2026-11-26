package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * "Contradiction Detective" — дополнительный уровень защиты от
 * ложноположительных результатов поверх RequirementSetAnalyzer: гарантирует,
 * что ни одно противоречие не представлено как доказанный конфликт без
 * цитаты от каждой из сторон и без достаточной уверенности модели.
 */
@Component
public class ContradictionDetector {

    private static final double CONFIRMED_CONFIDENCE_THRESHOLD = 0.75;

    public List<ContradictionRecord> refine(List<ContradictionRecord> raw) {
        List<ContradictionRecord> refined = new ArrayList<>();
        for (ContradictionRecord record : raw) {
            EvidenceStatus status = record.getStatus();

            boolean hasQuoteForEachSide = record.getQuotes() != null
                    && record.getRequirementIds().stream().allMatch(id ->
                    record.getQuotes().get(id) != null && !record.getQuotes().get(id).isBlank());

            if (!hasQuoteForEachSide && status == EvidenceStatus.CONFIRMED_ISSUE) {
                status = EvidenceStatus.NEEDS_HUMAN_REVIEW;
                record.setStatus(status);
            }
            if (status == EvidenceStatus.CONFIRMED_ISSUE && record.getConfidence() < CONFIRMED_CONFIDENCE_THRESHOLD) {
                record.setStatus(EvidenceStatus.POTENTIAL_ISSUE);
            }
            refined.add(record);
        }
        return refined;
    }
}
