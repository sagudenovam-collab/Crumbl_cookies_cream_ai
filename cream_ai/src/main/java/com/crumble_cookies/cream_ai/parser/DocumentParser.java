package com.crumble_cookies.cream_ai.parser;

import java.nio.file.Path;

/** Парсер документа одного формата (PDF, Markdown, DOCX, TXT). */
public interface DocumentParser {

    /** Поддерживает ли парсер данный файл (по расширению). */
    boolean supports(String fileName);

    /** Разобрать документ. Бросает DocumentParsingException на повреждённых/некорректных файлах. */
    ParsedDocument parse(Path path, String fileName) throws DocumentParsingException;
}
