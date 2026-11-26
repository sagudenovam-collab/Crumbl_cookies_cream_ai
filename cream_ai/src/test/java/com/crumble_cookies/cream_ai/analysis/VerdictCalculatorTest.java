package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.Severity;
import com.crumble_cookies.cream_ai.model.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VerdictCalculatorTest {

    private final VerdictCalculator calculator = new VerdictCalculator();

    @Test
    void emptyDocumentIsInsufficientInformation() {
        var result = calculator.calculate(List.of(), List.of(), List.of(), List.of(), 0);
        assertThat(result.verdict()).isEqualTo(Verdict.INSUFFICIENT_INFORMATION);
    }

    @Test
    void confirmedCriticalDefectMakesNotReadyRegardlessOfScore() {
        Requirement req = Requirement.builder().internalId("REQ-001").build();
        Finding critical = Finding.builder().status(EvidenceStatus.CONFIRMED_ISSUE).severity(Severity.CRITICAL).build();

        var result = calculator.calculate(List.of(req), List.of(critical), List.of(), List.of(), 95);

        assertThat(result.verdict()).isEqualTo(Verdict.NOT_READY);
    }

    @Test
    void cleanDocumentIsReady() {
        Requirement req = Requirement.builder().internalId("REQ-001").build();
        var result = calculator.calculate(List.of(req), List.of(), List.of(), List.of(), 97);
        assertThat(result.verdict()).isEqualTo(Verdict.READY);
    }

    @Test
    void mostlyUnverifiedFindingsAreInsufficientInformation() {
        Requirement r1 = Requirement.builder().internalId("REQ-001").needsHumanReview(true).build();
        Requirement r2 = Requirement.builder().internalId("REQ-002").needsHumanReview(true).build();
        var result = calculator.calculate(List.of(r1, r2), List.of(), List.of(), List.of(), 60);
        assertThat(result.verdict()).isEqualTo(Verdict.INSUFFICIENT_INFORMATION);
    }
}
