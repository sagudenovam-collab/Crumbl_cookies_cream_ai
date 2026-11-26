package com.crumble_cookies.cream_ai.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Парсер DOCX-документов (Apache POI). Номера страниц/строк в DOCX не
 * определяются средствами POI, поэтому местоположение не заполняется
 * (не придумываем координаты, которых система фактически не определила) —
 * вместо этого сохраняется порядковый номер абзаца как ориентир.
 */
@Component
public class DocxDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".docx");
    }

    @Override
    public ParsedDocument parse(Path path, String fileName) throws DocumentParsingException {
        try (FileInputStream fis = new FileInputStream(path.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            List<DocumentSegment> segments = new ArrayList<>();
            int paragraphIndex = 0;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                paragraphIndex++;
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    segments.add(new DocumentSegment(text.trim(), null, paragraphIndex));
                }
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    paragraphIndex++;
                    StringBuilder rowText = new StringBuilder();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        if (rowText.length() > 0) {
                            rowText.append(" | ");
                        }
                        rowText.append(cell.getText());
                    }
                    if (!rowText.toString().isBlank()) {
                        segments.add(new DocumentSegment(rowText.toString().trim(), null, paragraphIndex));
                    }
                }
            }
            return new ParsedDocument(fileName, segments, null);
        } catch (IOException e) {
            throw new DocumentParsingException("Не удалось прочитать DOCX-файл (возможно, он повреждён): " + fileName, e);
        } catch (Exception e) {
            throw new DocumentParsingException("Непредвиденная ошибка при разборе DOCX-файла: " + fileName, e);
        }
    }
}
