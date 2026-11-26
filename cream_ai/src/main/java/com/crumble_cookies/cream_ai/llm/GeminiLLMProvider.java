package com.crumble_cookies.cream_ai.llm;

import com.crumble_cookies.cream_ai.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

/**
 * Реализация LLMProvider поверх Gemini API (generateContent).
 * Обрабатывает лимиты запросов, тайм-ауты, ошибки сети и квоты, делает
 * повторные попытки с ограниченным количеством попыток (экспоненциальная задержка).
 * Если внешний LLM недоступен, бросает LLMException — фиктивный "успешный"
 * результат никогда не подставляется.
 */
@Component
public class GeminiLLMProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiLLMProvider.class);

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiLLMProvider(GeminiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) throws LLMException {
        if (!properties.isConfigured()) {
            throw new LLMException(LLMException.Kind.AUTH_ERROR,
                    "Не настроен GEMINI_API_KEY. Укажите ключ в переменной окружения или файле .env.");
        }

        String url = "/models/%s:generateContent?key=%s".formatted(properties.getModel(), properties.getApiKey());
        ObjectNode body = buildRequestBody(systemPrompt, userPrompt);

        int attempt = 0;
        LLMException lastError = null;
        while (attempt < Math.max(1, properties.getMaxRetries())) {
            attempt++;
            try {
                String rawResponse = restClient.post()
                        .uri(url)
                        .body(body)
                        .retrieve()
                        .body(String.class);
                return extractText(rawResponse);
            } catch (HttpClientErrorException e) {
                HttpStatusCode status = e.getStatusCode();
                if (status.value() == 429) {
                    lastError = new LLMException(LLMException.Kind.RATE_LIMIT,
                            "Превышен лимит запросов к Gemini API (HTTP 429).", e);
                } else if (status.value() == 401 || status.value() == 403) {
                    throw new LLMException(LLMException.Kind.AUTH_ERROR,
                            "Gemini API отклонил ключ доступа (HTTP " + status.value() + ").", e);
                } else if (status.value() == 402) {
                    throw new LLMException(LLMException.Kind.QUOTA_EXCEEDED,
                            "Исчерпана квота Gemini API.", e);
                } else {
                    lastError = new LLMException(LLMException.Kind.INVALID_RESPONSE,
                            "Gemini API вернул ошибку клиента: HTTP " + status.value() + ": " + e.getResponseBodyAsString(), e);
                }
            } catch (HttpServerErrorException e) {
                lastError = new LLMException(LLMException.Kind.NETWORK_ERROR,
                        "Gemini API временно недоступен (HTTP " + e.getStatusCode().value() + ").", e);
            } catch (ResourceAccessException e) {
                lastError = new LLMException(LLMException.Kind.TIMEOUT,
                        "Превышено время ожидания ответа от Gemini API.", e);
            }

            if (attempt < properties.getMaxRetries()) {
                long backoffMillis = Duration.ofSeconds((long) Math.pow(2, attempt)).toMillis();
                log.warn("Повторная попытка обращения к Gemini API #{} через {} мс. Причина: {}",
                        attempt + 1, backoffMillis, lastError != null ? lastError.getMessage() : "неизвестно");
                sleep(backoffMillis);
            }
        }
        throw lastError != null
                ? lastError
                : new LLMException(LLMException.Kind.UNKNOWN, "Не удалось получить ответ от Gemini API.");
    }

    private ObjectNode buildRequestBody(String systemPrompt, String userPrompt) {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode systemInstruction = objectMapper.createObjectNode();
        ArrayNode systemParts = objectMapper.createArrayNode();
        systemParts.add(objectMapper.createObjectNode().put("text", systemPrompt));
        systemInstruction.set("parts", systemParts);
        root.set("systemInstruction", systemInstruction);

        ArrayNode contents = objectMapper.createArrayNode();
        ObjectNode userContent = objectMapper.createObjectNode();
        userContent.put("role", "user");
        ArrayNode userParts = objectMapper.createArrayNode();
        userParts.add(objectMapper.createObjectNode().put("text", userPrompt));
        userContent.set("parts", userParts);
        contents.add(userContent);
        root.set("contents", contents);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("temperature", properties.getTemperature());
        generationConfig.put("responseMimeType", "application/json");
        root.set("generationConfig", generationConfig);

        return root;
    }

    private String extractText(String rawResponse) throws LLMException {
        try {
            JsonNode node = objectMapper.readTree(rawResponse);
            JsonNode candidates = node.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                JsonNode promptFeedback = node.path("promptFeedback");
                throw new LLMException(LLMException.Kind.INVALID_RESPONSE,
                        "Gemini API не вернул кандидатов ответа. promptFeedback=" + promptFeedback);
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    sb.append(part.path("text").asText(""));
                }
            }
            if (sb.isEmpty()) {
                throw new LLMException(LLMException.Kind.INVALID_RESPONSE,
                        "Пустой ответ модели Gemini.");
            }
            return sb.toString();
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException(LLMException.Kind.INVALID_RESPONSE,
                    "Не удалось разобрать ответ Gemini API: " + e.getMessage(), e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
