package com.crumble_cookies.cream_ai.llm;

/** Утилита для надёжного извлечения JSON из текстового ответа LLM. */
public final class JsonExtractionUtil {

    private JsonExtractionUtil() {
    }

    /**
     * Извлекает JSON-объект/массив из ответа модели, снимая markdown code fences
     * (```json ... ```), если они присутствуют, и обрезая случайный текст до/после.
     */
    public static String extractJson(String raw) throws LLMException {
        if (raw == null || raw.isBlank()) {
            throw new LLMException(LLMException.Kind.INVALID_RESPONSE, "Пустой ответ модели.");
        }
        String text = raw.trim();

        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            }
            int fenceEnd = text.lastIndexOf("```");
            if (fenceEnd != -1) {
                text = text.substring(0, fenceEnd);
            }
            text = text.trim();
        }

        int firstObj = text.indexOf('{');
        int firstArr = text.indexOf('[');
        int start;
        char openChar;
        char closeChar;
        if (firstObj == -1 && firstArr == -1) {
            throw new LLMException(LLMException.Kind.INVALID_RESPONSE,
                    "В ответе модели не найден JSON: " + truncate(raw));
        } else if (firstArr == -1 || (firstObj != -1 && firstObj < firstArr)) {
            start = firstObj;
            openChar = '{';
            closeChar = '}';
        } else {
            start = firstArr;
            openChar = '[';
            closeChar = ']';
        }

        int depth = 0;
        int end = -1;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }

        if (end == -1) {
            throw new LLMException(LLMException.Kind.INVALID_RESPONSE,
                    "Не удалось найти корректно закрытый JSON в ответе модели: " + truncate(raw));
        }
        return text.substring(start, end + 1);
    }

    private static String truncate(String s) {
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
