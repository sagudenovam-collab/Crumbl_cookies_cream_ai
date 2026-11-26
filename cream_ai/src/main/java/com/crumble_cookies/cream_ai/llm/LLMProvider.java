package com.crumble_cookies.cream_ai.llm;

/**
 * Абстракция над провайдером LLM. Архитектура не привязана к одному
 * поставщику — Gemini можно заменить другой реализацией без переписывания
 * остальной системы.
 */
public interface LLMProvider {

    /**
     * Выполнить запрос к модели и вернуть текстовый ответ.
     *
     * @param systemPrompt системная инструкция (роль, правила, формат ответа)
     * @param userPrompt   содержательный запрос (документ/требования для анализа)
     * @return текст ответа модели (ожидается JSON согласно systemPrompt)
     */
    String complete(String systemPrompt, String userPrompt) throws LLMException;

    /** Название модели, используемое в паспорте анализа. */
    String modelName();
}
