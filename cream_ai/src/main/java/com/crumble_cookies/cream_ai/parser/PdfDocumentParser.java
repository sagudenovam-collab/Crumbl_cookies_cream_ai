package com.crumble_cookies.cream_ai.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Парсер PDF-документов с сохранением номера страницы для каждого фрагмента. */
@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    @Override
    public ParsedDocument parse(Path path, String fileName) throws DocumentParsingException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            if (document.isEncrypted()) {
                throw new DocumentParsingException("PDF-файл зашифрован и не может быть прочитан: " + fileName);
            }
            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                return new ParsedDocument(fileName, List.of(), 0);
            }

            List<DocumentSegment> segments = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);
                if (pageText == null) {
                    continue;
                }
                for (String paragraph : splitIntoParagraphs(pageText)) {
                    if (!paragraph.isBlank()) {
                        segments.add(new DocumentSegment(paragraph.trim(), i, null));
                    }
                }
            }
            return new ParsedDocument(fileName, segments, pageCount);
        } catch (IOException e) {
            throw new DocumentParsingException("Не удалось прочитать PDF-файл (возможно, он повреждён): " + fileName, e);
        } catch (DocumentParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentParsingException("Непредвиденная ошибка при разборе PDF-файла: " + fileName, e);
        }
    }

    private List<String> splitIntoParagraphs(String pageText) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : pageText.split("\\r?\\n")) {
            if (line.isBlank()) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(line.trim());
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }
}
