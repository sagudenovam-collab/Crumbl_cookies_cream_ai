package com.crumble_cookies.cream_ai.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfDocumentParserTest {

    private final PdfDocumentParser parser = new PdfDocumentParser();

    @Test
    void supportsPdfExtensionOnly() {
        assertThat(parser.supports("doc.pdf")).isTrue();
        assertThat(parser.supports("doc.PDF")).isTrue();
        assertThat(parser.supports("doc.docx")).isFalse();
    }

    @Test
    void parsesTextAndKeepsPageNumbers(@TempDir Path dir) throws Exception {
        Path pdfFile = dir.resolve("sample.pdf");
        createTwoPagePdf(pdfFile, "The system shall support PDF parsing.", "Second requirement on second page.");

        ParsedDocument doc = parser.parse(pdfFile, "sample.pdf");

        assertThat(doc.totalPages()).isEqualTo(2);
        assertThat(doc.segments()).anySatisfy(s -> {
            assertThat(s.text()).contains("PDF");
            assertThat(s.page()).isEqualTo(1);
        });
        assertThat(doc.segments()).anySatisfy(s -> assertThat(s.page()).isEqualTo(2));
    }

    @Test
    void corruptedPdfThrowsDocumentParsingException(@TempDir Path dir) throws Exception {
        Path brokenFile = dir.resolve("broken.pdf");
        Files.writeString(brokenFile, "это не настоящий PDF файл");

        assertThatThrownBy(() -> parser.parse(brokenFile, "broken.pdf"))
                .isInstanceOf(DocumentParsingException.class);
    }

    @Test
    void emptyPdfProducesNoSegments(@TempDir Path dir) throws Exception {
        Path pdfFile = dir.resolve("empty.pdf");
        try (PDDocument document = new PDDocument()) {
            document.save(pdfFile.toFile());
        }

        ParsedDocument doc = parser.parse(pdfFile, "empty.pdf");

        assertThat(doc.isEmpty()).isTrue();
    }

    private void createTwoPagePdf(Path path, String page1Text, String page2Text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            addPage(document, page1Text);
            addPage(document, page2Text);
            document.save(path.toFile());
        }
    }

    private void addPage(PDDocument document, String text) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            stream.newLineAtOffset(50, 700);
            stream.showText(text);
            stream.endText();
        }
    }
}
