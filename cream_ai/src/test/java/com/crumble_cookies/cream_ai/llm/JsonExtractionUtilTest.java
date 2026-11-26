package com.crumble_cookies.cream_ai.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonExtractionUtilTest {

    @Test
    void extractsPlainJson() throws Exception {
        String result = JsonExtractionUtil.extractJson("{\"a\": 1}");
        assertThat(result).isEqualTo("{\"a\": 1}");
    }

    @Test
    void stripsMarkdownCodeFences() throws Exception {
        String raw = "```json\n{\"a\": 1}\n```";
        String result = JsonExtractionUtil.extractJson(raw);
        assertThat(result).isEqualTo("{\"a\": 1}");
    }

    @Test
    void extractsJsonArrayIgnoringSurroundingText() throws Exception {
        String raw = "Вот результат:\n[{\"a\": 1}, {\"b\": 2}]\nСпасибо.";
        String result = JsonExtractionUtil.extractJson(raw);
        assertThat(result).isEqualTo("[{\"a\": 1}, {\"b\": 2}]");
    }

    @Test
    void throwsOnNonJsonResponse() {
        assertThatThrownBy(() -> JsonExtractionUtil.extractJson("просто текст без JSON"))
                .isInstanceOf(LLMException.class);
    }

    @Test
    void throwsOnBlankResponse() {
        assertThatThrownBy(() -> JsonExtractionUtil.extractJson(""))
                .isInstanceOf(LLMException.class);
    }
}
