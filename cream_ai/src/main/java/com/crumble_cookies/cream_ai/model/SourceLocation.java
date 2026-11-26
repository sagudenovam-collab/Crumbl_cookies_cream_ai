package com.crumble_cookies.cream_ai.model;

/**
 * Местоположение фрагмента в исходном документе.
 * Страница заполняется для PDF, номер строки — для Markdown/TXT.
 * Ни одно из полей не придумывается: если система не определила точное
 * местоположение, соответствующее поле остаётся null.
 */
public record SourceLocation(String sourceFile, Integer page, Integer line) {

    public static SourceLocation unknown(String sourceFile) {
        return new SourceLocation(sourceFile, null, null);
    }

    public String describe() {
        if (page != null) {
            return "стр. " + page;
        }
        if (line != null) {
            return "строка " + line;
        }
        return "местоположение не определено";
    }
}
