package com.crumble_cookies.cream_ai.llm;

/** Ошибка обращения к внешнему LLM-провайдеру (сеть, квота, некорректный ответ и т.п.). */
public class LLMException extends Exception {

    public enum Kind {
        NETWORK_ERROR,
        RATE_LIMIT,
        QUOTA_EXCEEDED,
        TIMEOUT,
        INVALID_RESPONSE,
        AUTH_ERROR,
        UNKNOWN
    }

    private final Kind kind;

    public LLMException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public LLMException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
