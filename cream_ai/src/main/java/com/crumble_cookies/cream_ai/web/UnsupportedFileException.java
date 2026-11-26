package com.crumble_cookies.cream_ai.web;

/** Файл не прошёл валидацию (неверный формат, пустой, слишком большой и т.п.). */
public class UnsupportedFileException extends RuntimeException {
    public UnsupportedFileException(String message) {
        super(message);
    }
}
