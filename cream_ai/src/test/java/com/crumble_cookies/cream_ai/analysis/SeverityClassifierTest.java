package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Priority;
import com.crumble_cookies.cream_ai.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeverityClassifierTest {

    private final SeverityClassifier classifier = new SeverityClassifier();

    @Test
    void mapsSeverityToPriority() {
        Finding critical = Finding.builder().severity(Severity.CRITICAL).status(EvidenceStatus.CONFIRMED_ISSUE).build();
        Finding low = Finding.builder().severity(Severity.LOW).status(EvidenceStatus.CONFIRMED_ISSUE).build();

        classifier.classify(List.of(critical, low));

        assertThat(critical.getPriority()).isEqualTo(Priority.P0);
        assertThat(low.getPriority()).isEqualTo(Priority.P3);
    }

    @Test
    void doesNotOverstateSeverityForUnverifiedEvidence() {
        Finding unverifiedCritical = Finding.builder()
                .severity(Severity.CRITICAL)
                .status(EvidenceStatus.NEEDS_HUMAN_REVIEW)
                .build();

        classifier.classify(List.of(unverifiedCritical));

        assertThat(unverifiedCritical.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(unverifiedCritical.getPriority()).isEqualTo(Priority.P2);
    }
}
