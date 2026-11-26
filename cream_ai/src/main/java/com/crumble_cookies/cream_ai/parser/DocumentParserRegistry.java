package com.crumble_cookies.cream_ai.parser;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Реестр парсеров документов: выбирает нужный парсер по расширению файла. */
@Component
public class DocumentParserRegistry {

    private final List<DocumentParser> parsers;

    public DocumentParserRegistry(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public ParsedDocument parse(Path path, String fileName) throws DocumentParsingException {
        if (fileName == null || fileName.isBlank()) {
            throw new DocumentParsingException("Имя файла не указано.");
        }
        for (DocumentParser parser : parsers) {
            if (parser.supports(fileName)) {
                return parser.parse(path, fileName);
            }
        }
        String ext = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT)
                : "(без расширения)";
        throw new DocumentParsingException("Неподдерживаемый формат файла: " + ext
                + ". Поддерживаются: PDF, Markdown (.md), DOCX, TXT.");
    }
}
