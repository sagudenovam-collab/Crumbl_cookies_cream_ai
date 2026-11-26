package com.crumble_cookies.cream_ai.web;

/** Анализ с указанным ID не найден. */
public class AnalysisNotFoundException extends RuntimeException {
    public AnalysisNotFoundException(String id) {
        super("Анализ не найден: " + id);
    }
}
