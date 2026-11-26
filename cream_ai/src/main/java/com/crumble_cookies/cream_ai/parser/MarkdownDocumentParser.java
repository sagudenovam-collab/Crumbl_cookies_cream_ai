package com.crumble_cookies.cream_ai.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Парсер Markdown/текстовых документов с сохранением номера строки.
 * Требования обычно оформляются как абзацы, пункты списков или строки таблиц,
 * поэтому разбиваем документ на логические блоки по пустым строкам, сохраняя
 * номер первой строки каждого блока.
 */
@Component
public class MarkdownDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    @Override
    public ParsedDocument parse(Path path, String fileName) throws DocumentParsingException {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<DocumentSegment> segments = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            int blockStartLine = -1;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineNo = i + 1;
                if (line.isBlank()) {
                    flush(segments, current, blockStartLine);
                    current.setLength(0);
                    blockStartLine = -1;
                } else {
                    if (blockStartLine == -1) {
                        blockStartLine = lineNo;
                    }
                    if (current.length() > 0) {
                        current.append(' ');
                    }
                    current.append(line.trim());
                }
            }
            flush(segments, current, blockStartLine);
            return new ParsedDocument(fileName, segments, null);
        } catch (java.nio.charset.MalformedInputException e) {
            throw new DocumentParsingException("Файл повреждён или имеет некорректную кодировку: " + fileName, e);
        } catch (IOException e) {
            throw new DocumentParsingException("Не удалось прочитать файл: " + fileName, e);
        }
    }

    private void flush(List<DocumentSegment> segments, StringBuilder current, int blockStartLine) {
        if (current.length() > 0) {
            segments.add(new DocumentSegment(current.toString(), null, blockStartLine));
        }
    }
}
