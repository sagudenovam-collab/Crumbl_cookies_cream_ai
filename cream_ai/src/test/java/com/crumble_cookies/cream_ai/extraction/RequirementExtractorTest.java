package com.crumble_cookies.cream_ai.extraction;

import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.parser.DocumentSegment;
import com.crumble_cookies.cream_ai.parser.ParsedDocument;
import com.crumble_cookies.cream_ai.support.FakeLLMProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementExtractorTest {

    @Test
    void mapsLlmCandidatesToRequirementsUsingRealSegmentLocations() throws Exception {
        ParsedDocument document = new ParsedDocument("req.md", List.of(
                new DocumentSegment("# Введение", null, 1),
                new DocumentSegment("REQ-10: Система должна отправлять email в течение 2 секунд.", null, 3),
                new DocumentSegment("Примечание: это просто пример, не требование.", null, 5)
        ), null);

        FakeLLMProvider fake = new FakeLLMProvider();
        fake.queueResponse("""
                [
                  {"originalId": null, "text": "Введение", "summary": "заголовок", "type": "UNKNOWN",
                   "segmentRefs": [0], "ambiguous": false, "ambiguousReason": null, "isActualRequirement": false},
                  {"originalId": "REQ-10", "text": "Система должна отправлять email в течение 2 секунд.",
                   "summary": "Отправка email за 2 секунды", "type": "FUNCTIONAL", "segmentRefs": [1],
                   "ambiguous": false, "ambiguousReason": null, "isActualRequirement": true},
                  {"originalId": null, "text": "Примечание: это просто пример, не требование.",
                   "summary": "пример", "type": "UNKNOWN", "segmentRefs": [2],
                   "ambiguous": true, "ambiguousReason": "похоже на пояснение, а не требование",
                   "isActualRequirement": true}
                ]
                """);

        RequirementExtractor extractor = new RequirementExtractor(fake);
        List<Requirement> result = extractor.extract(document);

        assertThat(result).hasSize(2); // заголовок отфильтрован, т.к. isActualRequirement=false
        Requirement withId = result.stream().filter(r -> "REQ-10".equals(r.getOriginalId())).findFirst().orElseThrow();
        assertThat(withId.getLocation().line()).isEqualTo(3);
        assertThat(withId.isNeedsHumanReview()).isFalse();

        Requirement ambiguous = result.stream().filter(Requirement::isNeedsHumanReview).findFirst().orElseThrow();
        assertThat(ambiguous.getHumanReviewReason()).contains("пояснение");
    }
}
