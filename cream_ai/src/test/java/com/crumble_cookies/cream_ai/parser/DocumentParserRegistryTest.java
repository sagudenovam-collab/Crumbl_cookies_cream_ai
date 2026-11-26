package com.crumble_cookies.cream_ai.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentParserRegistryTest {

    private final DocumentParserRegistry registry = new DocumentParserRegistry(
            List.of(new PdfDocumentParser(), new MarkdownDocumentParser(),
                    new DocxDocumentParser(), new PlainTextDocumentParser()));

    @Test
    void unsupportedFormatThrowsClearError(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("image.png");
        Files.write(file, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> registry.parse(file, "image.png"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Неподдерживаемый формат");
    }

    @Test
    void blankFileNameThrows(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("x");
        Files.writeString(file, "test");

        assertThatThrownBy(() -> registry.parse(file, ""))
                .isInstanceOf(DocumentParsingException.class);
    }
}
