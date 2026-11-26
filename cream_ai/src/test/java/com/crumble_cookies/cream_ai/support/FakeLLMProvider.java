package com.crumble_cookies.cream_ai.support;

import com.crumble_cookies.cream_ai.llm.LLMException;
import com.crumble_cookies.cream_ai.llm.LLMProvider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Тестовый двойник LLMProvider: возвращает заранее запрограммированные ответы
 * по очереди, не обращаясь к сети. Позволяет детерминированно тестировать
 * конвейер анализа без реального Gemini API.
 */
public class FakeLLMProvider implements LLMProvider {

    private final Deque<String> responses = new ArrayDeque<>();
    private final List<String> receivedUserPrompts = new ArrayList<>();
    private LLMException nextError;

    public FakeLLMProvider queueResponse(String json) {
        responses.addLast(json);
        return this;
    }

    public FakeLLMProvider failNext(LLMException error) {
        this.nextError = error;
        return this;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) throws LLMException {
        receivedUserPrompts.add(userPrompt);
        if (nextError != null) {
            LLMException e = nextError;
            nextError = null;
            throw e;
        }
        if (responses.isEmpty()) {
            throw new LLMException(LLMException.Kind.UNKNOWN, "FakeLLMProvider: нет запрограммированного ответа");
        }
        return responses.pollFirst();
    }

    @Override
    public String modelName() {
        return "fake-model-for-tests";
    }

    public List<String> getReceivedUserPrompts() {
        return receivedUserPrompts;
    }
}
