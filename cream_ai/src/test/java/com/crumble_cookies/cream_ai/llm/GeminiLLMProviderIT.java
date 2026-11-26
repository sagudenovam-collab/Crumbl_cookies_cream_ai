package com.crumble_cookies.cream_ai.llm;

import com.crumble_cookies.cream_ai.config.GeminiProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Интеграционный тест с РЕАЛЬНЫМ Gemini API. Отделён от обычного набора тестов
 * тегом "requires-gemini-api" (исключён из surefire по умолчанию, см. pom.xml),
 * чтобы сборка проходила без ключа API. Запуск:
 * mvn test -Dgroups=requires-gemini-api -DGEMINI_API_KEY=...
 */
@Tag("requires-gemini-api")
class GeminiLLMProviderIT {

    @Test
    void realApiReturnsValidJsonForSimplePrompt() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY не задан — пропускаем интеграционный тест");

        GeminiProperties props = new GeminiProperties();
        props.setApiKey(apiKey);
        GeminiLLMProvider provider = new GeminiLLMProvider(props);

        String response = provider.complete(
                "Ты отвечаешь строго в формате JSON: {\"ok\": true}. Никакого другого текста.",
                "Подтверди, что ты работаешь.");

        assertThat(response).contains("ok");
    }
}
