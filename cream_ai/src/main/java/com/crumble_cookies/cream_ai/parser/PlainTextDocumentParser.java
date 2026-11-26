package com.crumble_cookies.cream_ai.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Парсер обычных текстовых файлов (.txt) с сохранением номера строки. */
@Component
public class PlainTextDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".txt");
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
                    if (current.length() > 0) {
                        segments.add(new DocumentSegment(current.toString(), null, blockStartLine));
                        current.setLength(0);
                    }
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
            if (current.length() > 0) {
                segments.add(new DocumentSegment(current.toString(), null, blockStartLine));
            }
            return new ParsedDocument(fileName, segments, null);
        } catch (IOException e) {
            throw new DocumentParsingException("Не удалось прочитать текстовый файл: " + fileName, e);
        }
    }
}
