package com.crumble_cookies.cream_ai.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PlainTextDocumentParserTest {

    private final PlainTextDocumentParser parser = new PlainTextDocumentParser();

    @Test
    void parsesLinesWithLineNumbers(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("req.txt");
        Files.writeString(file, "Требование первое.\n\nТребование второе.\n");

        ParsedDocument doc = parser.parse(file, "req.txt");

        assertThat(doc.segments()).hasSize(2);
        assertThat(doc.segments().get(1).line()).isEqualTo(3);
    }
}
