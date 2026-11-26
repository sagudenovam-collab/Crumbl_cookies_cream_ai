package com.crumble_cookies.cream_ai.extraction;

import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.SourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementNormalizerTest {

    private final RequirementNormalizer normalizer = new RequirementNormalizer();

    @Test
    void assignsSequentialInternalIdsAndDropsExactDuplicates() {
        Requirement r1 = Requirement.builder().originalText("Текст A").location(SourceLocation.unknown("f")).build();
        Requirement r2 = Requirement.builder().originalText("Текст B").location(SourceLocation.unknown("f")).build();
        Requirement r3Dup = Requirement.builder().originalText("Текст A").location(SourceLocation.unknown("f")).build();

        List<Requirement> result = normalizer.normalize(List.of(r1, r2, r3Dup));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getInternalId()).isEqualTo("REQ-001");
        assertThat(result.get(1).getInternalId()).isEqualTo("REQ-002");
    }

    @Test
    void skipsBlankText() {
        Requirement blank = Requirement.builder().originalText("   ").build();
        List<Requirement> result = normalizer.normalize(List.of(blank));
        assertThat(result).isEmpty();
    }
}
