package com.crumble_cookies.cream_ai.web;

/** Результаты запрошены раньше, чем анализ завершился. */
public class AnalysisNotReadyException extends RuntimeException {
    public AnalysisNotReadyException(String currentStatus) {
        super("Анализ ещё не завершён. Текущий статус: " + currentStatus);
    }
}
