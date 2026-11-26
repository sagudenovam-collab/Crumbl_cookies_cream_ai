package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.SourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceVerifierTest {

    private final EvidenceVerifier verifier = new EvidenceVerifier();

    private Requirement requirement(String text) {
        return Requirement.builder().internalId("REQ-001").originalText(text)
                .location(SourceLocation.unknown("f")).build();
    }

    @Test
    void quoteFoundWithHighConfidenceIsConfirmed() {
        Requirement req = requirement("Система должна отвечать быстро.");
        Finding f = Finding.builder().id("F1").relatedRequirementIds(List.of("REQ-001"))
                .quote("отвечать быстро").confidence(0.9).build();

        verifier.verifyIndividualFindings(List.of(f), Map.of("REQ-001", req));

        assertThat(f.getStatus()).isEqualTo(EvidenceStatus.CONFIRMED_ISSUE);
    }

    @Test
    void quoteNotFoundInDocumentIsNeedsHumanReview() {
        Requirement req = requirement("Система должна отвечать быстро.");
        Finding f = Finding.builder().id("F2").relatedRequirementIds(List.of("REQ-001"))
                .quote("этой фразы нет в тексте").confidence(0.95).build();

        verifier.verifyIndividualFindings(List.of(f), Map.of("REQ-001", req));

        assertThat(f.getStatus()).isEqualTo(EvidenceStatus.NEEDS_HUMAN_REVIEW);
    }

    @Test
    void quoteFoundButLowConfidenceIsPotential() {
        Requirement req = requirement("Система должна отвечать быстро.");
        Finding f = Finding.builder().id("F3").relatedRequirementIds(List.of("REQ-001"))
                .quote("отвечать быстро").confidence(0.5).build();

        verifier.verifyIndividualFindings(List.of(f), Map.of("REQ-001", req));

        assertThat(f.getStatus()).isEqualTo(EvidenceStatus.POTENTIAL_ISSUE);
    }

    @Test
    void quoteMatchIsCaseAndWhitespaceInsensitive() {
        assertThat(verifier.quoteFoundIn("Отвечать   Быстро", "система должна отвечать быстро всегда")).isTrue();
    }
}
