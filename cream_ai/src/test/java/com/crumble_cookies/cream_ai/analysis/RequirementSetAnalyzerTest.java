package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.support.FakeLLMProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementSetAnalyzerTest {

    @Test
    void detectsContradictionBetweenTwoRequirements() throws Exception {
        FakeLLMProvider fake = new FakeLLMProvider();
        fake.queueResponse("""
                {
                  "contradictions": [
                    {"requirementIds": ["REQ-001", "REQ-002"],
                     "quotes": {"REQ-001": "ответ в течение 2 секунд", "REQ-002": "ответ в течение 10 секунд"},
                     "explanation": "разное время отклика для одного и того же действия",
                     "possibleConsequence": "неясно, какое время закладывать в разработку",
                     "resolutionSuggestion": "уточнить единое значение у заказчика",
                     "status": "CONFIRMED_ISSUE", "confidence": 0.9}
                  ],
                  "duplicates": [],
                  "other": []
                }
                """);
        RequirementSetAnalyzer analyzer = new RequirementSetAnalyzer(fake);

        Requirement r1 = Requirement.builder().internalId("REQ-001")
                .originalText("Система должна отвечать в течение 2 секунд.").build();
        Requirement r2 = Requirement.builder().internalId("REQ-002")
                .originalText("Система должна отвечать в течение 10 секунд.").build();

        RequirementSetAnalyzer.SetAnalysisOutput output = analyzer.analyze(List.of(r1, r2));

        assertThat(output.contradictions()).hasSize(1);
        assertThat(output.contradictions().get(0).getRequirementIds()).containsExactly("REQ-001", "REQ-002");
    }

    @Test
    void detectsDuplicateRequirements() throws Exception {
        FakeLLMProvider fake = new FakeLLMProvider();
        fake.queueResponse("""
                {
                  "contradictions": [],
                  "duplicates": [
                    {"requirementIds": ["REQ-001", "REQ-002"], "explanation": "оба требования описывают одно и то же",
                     "status": "CONFIRMED_ISSUE", "confidence": 0.85}
                  ],
                  "other": []
                }
                """);
        RequirementSetAnalyzer analyzer = new RequirementSetAnalyzer(fake);

        Requirement r1 = Requirement.builder().internalId("REQ-001").originalText("Пользователь может выйти из системы.").build();
        Requirement r2 = Requirement.builder().internalId("REQ-002").originalText("Пользователь может завершить сеанс.").build();

        RequirementSetAnalyzer.SetAnalysisOutput output = analyzer.analyze(List.of(r1, r2));

        assertThat(output.duplicates()).hasSize(1);
    }

    @Test
    void ignoresHallucinatedRequirementIds() throws Exception {
        FakeLLMProvider fake = new FakeLLMProvider();
        fake.queueResponse("""
                {
                  "contradictions": [
                    {"requirementIds": ["REQ-999", "REQ-001"], "quotes": {}, "explanation": "x",
                     "possibleConsequence": "x", "resolutionSuggestion": "x", "status": "CONFIRMED_ISSUE", "confidence": 0.9}
                  ],
                  "duplicates": [], "other": []
                }
                """);
        RequirementSetAnalyzer analyzer = new RequirementSetAnalyzer(fake);
        Requirement r1 = Requirement.builder().internalId("REQ-001").originalText("Текст.").build();

        RequirementSetAnalyzer.SetAnalysisOutput output = analyzer.analyze(List.of(r1));

        // REQ-999 не существует -> недостаточно валидных ID для пары -> запись отбрасывается
        assertThat(output.contradictions()).isEmpty();
    }
}
