package com.crumble_cookies.cream_ai.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownDocumentParserTest {

    private final MarkdownDocumentParser parser = new MarkdownDocumentParser();

    @Test
    void supportsMdExtension() {
        assertThat(parser.supports("doc.md")).isTrue();
        assertThat(parser.supports("doc.MARKDOWN")).isTrue();
        assertThat(parser.supports("doc.txt")).isFalse();
    }

    @Test
    void parsesParagraphsWithLineNumbers(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("req.md");
        Files.writeString(file, """
                # Заголовок документа

                Система должна позволять пользователю войти в систему по логину и паролю.

                Система должна отправлять уведомление в течение 2 секунд после события.
                """);

        ParsedDocument doc = parser.parse(file, "req.md");

        assertThat(doc.segments()).isNotEmpty();
        assertThat(doc.segments()).anySatisfy(s -> assertThat(s.text()).contains("войти в систему"));
        assertThat(doc.segments()).allSatisfy(s -> assertThat(s.line()).isNotNull());
    }

    @Test
    void emptyDocumentProducesNoSegments(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("empty.md");
        Files.writeString(file, "");

        ParsedDocument doc = parser.parse(file, "empty.md");

        assertThat(doc.isEmpty()).isTrue();
    }
}
