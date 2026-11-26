package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContradictionDetectorTest {

    private final ContradictionDetector detector = new ContradictionDetector();

    @Test
    void demotesConfirmedContradictionWithoutQuoteForBothSides() {
        ContradictionRecord record = ContradictionRecord.builder()
                .id("CTR-1")
                .requirementIds(List.of("REQ-001", "REQ-002"))
                .quotes(Map.of("REQ-001", "цитата A")) // отсутствует цитата для REQ-002
                .status(EvidenceStatus.CONFIRMED_ISSUE)
                .confidence(0.9)
                .build();

        List<ContradictionRecord> result = detector.refine(List.of(record));

        assertThat(result.get(0).getStatus()).isEqualTo(EvidenceStatus.NEEDS_HUMAN_REVIEW);
    }

    @Test
    void demotesConfirmedContradictionWithLowConfidence() {
        ContradictionRecord record = ContradictionRecord.builder()
                .id("CTR-2")
                .requirementIds(List.of("REQ-001", "REQ-002"))
                .quotes(Map.of("REQ-001", "A", "REQ-002", "B"))
                .status(EvidenceStatus.CONFIRMED_ISSUE)
                .confidence(0.4)
                .build();

        List<ContradictionRecord> result = detector.refine(List.of(record));

        assertThat(result.get(0).getStatus()).isEqualTo(EvidenceStatus.POTENTIAL_ISSUE);
    }

    @Test
    void keepsWellEvidencedConfirmedContradiction() {
        ContradictionRecord record = ContradictionRecord.builder()
                .id("CTR-3")
                .requirementIds(List.of("REQ-001", "REQ-002"))
                .quotes(Map.of("REQ-001", "A", "REQ-002", "B"))
                .status(EvidenceStatus.CONFIRMED_ISSUE)
                .confidence(0.95)
                .build();

        List<ContradictionRecord> result = detector.refine(List.of(record));

        assertThat(result.get(0).getStatus()).isEqualTo(EvidenceStatus.CONFIRMED_ISSUE);
    }
}
