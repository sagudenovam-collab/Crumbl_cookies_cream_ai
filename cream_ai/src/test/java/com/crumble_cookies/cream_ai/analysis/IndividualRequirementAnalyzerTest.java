package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.llm.LLMException;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.SourceLocation;
import com.crumble_cookies.cream_ai.support.FakeLLMProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndividualRequirementAnalyzerTest {

    @Test
    void wellFormedRequirementProducesNoFindings() throws Exception {
        FakeLLMProvider fake = new FakeLLMProvider();
        fake.queueResponse("""
                {"results": [{"requirementId": "REQ-001", "qualityOk": true, "findings": []}]}
                """);
        IndividualRequirementAnalyzer analyzer = new IndividualRequirementAnalyzer(fake);

        Requirement req = Requirement.builder().internalId("REQ-001")
                .originalText("Система должна отправлять подтверждение регистрации по email в течение 5 секунд.")
                .location(SourceLocation.unknown("f")).build();

        List<Finding> findings = analyzer.analyze(List.of(req));

        assertThat(findings).isEmpty();
    }

    @Test
    void ambiguousRequirementProducesFindingWithQuote() throws Exception {
        FakeLLMProvider fake = new FakeLLMProvider();
        fake.queueResponse("""
                {"results": [{"requirementId": "REQ-002", "qualityOk": false, "findings": [
                   {"criterion": "Однозначность", "quote": "быстро", "explanation": "неясно, что значит быстро",
                    "severity": "MEDIUM", "severityReason": "нет измеримого критерия", "confidence": 0.8,
                    "recommendation": "указать точное время отклика", "improvedWording": null}
                ]}]}
                """);
        IndividualRequirementAnalyzer analyzer = new IndividualRequirementAnalyzer(fake);

        Requirement req = Requirement.builder().internalId("REQ-002")
                .originalText("Система должна работать быстро.")
                .location(SourceLocation.unknown("f")).build();

        List<Finding> findings = analyzer.analyze(List.of(req));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getQuote()).isEqualTo("быстро");
    }

    @Test
    void llmErrorPropagatesInsteadOfFakeSuccess() {
        FakeLLMProvider fake = new FakeLLMProvider();
        fake.failNext(new LLMException(LLMException.Kind.RATE_LIMIT, "429"));
        IndividualRequirementAnalyzer analyzer = new IndividualRequirementAnalyzer(fake);

        Requirement req = Requirement.builder().internalId("REQ-003").originalText("Текст").build();

        assertThatThrownBy(() -> analyzer.analyze(List.of(req))).isInstanceOf(LLMException.class);
    }

    @Test
    void malformedJsonResponseThrowsInvalidResponse() {
        FakeLLMProvider fake = new FakeLLMProvider();
        fake.queueResponse("это не json вообще");
        IndividualRequirementAnalyzer analyzer = new IndividualRequirementAnalyzer(fake);

        Requirement req = Requirement.builder().internalId("REQ-004").originalText("Текст").build();

        assertThatThrownBy(() -> analyzer.analyze(List.of(req)))
                .isInstanceOf(LLMException.class)
                .satisfies(e -> assertThat(((LLMException) e).kind()).isEqualTo(LLMException.Kind.INVALID_RESPONSE));
    }
}
