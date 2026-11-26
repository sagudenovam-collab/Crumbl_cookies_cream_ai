package com.crumble_cookies.cream_ai.parser;

/**
 * Фрагмент распарсенного документа: один абзац/строка/ячейка с привязкой
 * к местоположению в исходном документе (страница для PDF, строка для
 * Markdown/TXT). Если местоположение не определено, соответствующее поле
 * остаётся null — координаты никогда не придумываются.
 */
public record DocumentSegment(String text, Integer page, Integer line) {
}
