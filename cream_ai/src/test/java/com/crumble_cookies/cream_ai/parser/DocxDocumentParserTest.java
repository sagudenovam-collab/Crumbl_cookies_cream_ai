package com.crumble_cookies.cream_ai.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocxDocumentParserTest {

    private final DocxDocumentParser parser = new DocxDocumentParser();

    @Test
    void supportsDocxExtension() {
        assertThat(parser.supports("doc.docx")).isTrue();
        assertThat(parser.supports("doc.doc")).isFalse();
    }

    @Test
    void parsesParagraphs(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("sample.docx");
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("Система должна поддерживать DOCX файлы.");
            try (FileOutputStream out = new FileOutputStream(file.toFile())) {
                document.write(out);
            }
        }

        ParsedDocument doc = parser.parse(file, "sample.docx");

        assertThat(doc.segments()).anySatisfy(s -> assertThat(s.text()).contains("DOCX"));
    }

    @Test
    void corruptedDocxThrowsException(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("broken.docx");
        Files.writeString(file, "not a real docx");

        assertThatThrownBy(() -> parser.parse(file, "broken.docx"))
                .isInstanceOf(DocumentParsingException.class);
    }
}
