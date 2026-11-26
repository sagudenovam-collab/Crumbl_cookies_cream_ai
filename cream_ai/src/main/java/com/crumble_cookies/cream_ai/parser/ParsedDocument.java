package com.crumble_cookies.cream_ai.parser;

import java.util.List;

/** Результат разбора исходного документа. */
public record ParsedDocument(String fileName, List<DocumentSegment> segments, Integer totalPages) {

    /** Полный текст документа (для проверки цитат верификатором). */
    public String fullText() {
        StringBuilder sb = new StringBuilder();
        for (DocumentSegment s : segments) {
            sb.append(s.text()).append('\n');
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return segments == null || segments.isEmpty() || fullText().isBlank();
    }
}
