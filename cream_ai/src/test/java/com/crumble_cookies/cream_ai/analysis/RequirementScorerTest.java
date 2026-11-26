package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.HealthScoreBreakdown;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementScorerTest {

    private final RequirementScorer scorer = new RequirementScorer();

    @Test
    void perfectRequirementsScoreCloseToMax() {
        Requirement r1 = Requirement.builder().internalId("REQ-001").build();
        Requirement r2 = Requirement.builder().internalId("REQ-002").build();

        HealthScoreBreakdown score = scorer.score(List.of(r1, r2), List.of(), List.of(), List.of(), List.of());

        assertThat(score.getOverallScore()).isEqualTo(100);
    }

    @Test
    void confirmedCriticalFindingsLowerTheScoreMoreThanLowSeverity() {
        Requirement r1 = Requirement.builder().internalId("REQ-001").build();

        Finding critical = Finding.builder().criterion("Однозначность").severity(Severity.CRITICAL)
                .status(EvidenceStatus.CONFIRMED_ISSUE).relatedRequirementIds(List.of("REQ-001")).build();
        Finding low = Finding.builder().criterion("Однозначность").severity(Severity.LOW)
                .status(EvidenceStatus.CONFIRMED_ISSUE).relatedRequirementIds(List.of("REQ-001")).build();

        HealthScoreBreakdown criticalScore = scorer.score(List.of(r1), List.of(critical), List.of(), List.of(), List.of());
        HealthScoreBreakdown lowScore = scorer.score(List.of(r1), List.of(low), List.of(), List.of(), List.of());

        assertThat(criticalScore.getOverallScore()).isLessThan(lowScore.getOverallScore());
    }

    @Test
    void unverifiedFindingsPenalizeLessThanConfirmedOnes() {
        Requirement r1 = Requirement.builder().internalId("REQ-001").build();

        Finding confirmed = Finding.builder().criterion("Полнота").severity(Severity.HIGH)
                .status(EvidenceStatus.CONFIRMED_ISSUE).relatedRequirementIds(List.of("REQ-001")).build();
        Finding needsReview = Finding.builder().criterion("Полнота").severity(Severity.HIGH)
                .status(EvidenceStatus.NEEDS_HUMAN_REVIEW).relatedRequirementIds(List.of("REQ-001")).build();

        HealthScoreBreakdown confirmedScore = scorer.score(List.of(r1), List.of(confirmed), List.of(), List.of(), List.of());
        HealthScoreBreakdown reviewScore = scorer.score(List.of(r1), List.of(needsReview), List.of(), List.of(), List.of());

        assertThat(confirmedScore.getOverallScore()).isLessThan(reviewScore.getOverallScore());
    }
}
