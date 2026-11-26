package com.crumble_cookies.cream_ai.parser;

/** Ошибка при разборе исходного документа (повреждённый файл, неподдерживаемый формат и т.п.). */
public class DocumentParsingException extends Exception {
    public DocumentParsingException(String message) {
        super(message);
    }

    public DocumentParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
