package com.crumble_cookies.cream_ai.cli;

import com.crumble_cookies.cream_ai.analysis.AnalysisOrchestrator;
import com.crumble_cookies.cream_ai.llm.LLMException;
import com.crumble_cookies.cream_ai.model.AnalysisResult;
import com.crumble_cookies.cream_ai.parser.DocumentParsingException;
import com.crumble_cookies.cream_ai.report.JsonReportWriter;
import com.crumble_cookies.cream_ai.report.MarkdownReportGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * CLI-режим: запускается, если среди аргументов есть --input.
 * Пример: ./mvnw spring-boot:run -Dspring-boot.run.arguments="--input=docs/requirements.pdf --output=reports/result.md"
 * После завершения работы в CLI-режиме процесс корректно завершается
 * (веб-сервер не поднимается), не оставляя повреждённый отчёт при аварийном
 * завершении — отчёт записывается только во временный файл, а затем атомарно
 * переименовывается в целевой путь.
 */
@Component
@Order(1)
public class CliRunner implements ApplicationRunner {

    private final AnalysisOrchestrator orchestrator;
    private final MarkdownReportGenerator markdownReportGenerator;
    private final JsonReportWriter jsonReportWriter;
    private final ConfigurableApplicationContext context;

    public CliRunner(AnalysisOrchestrator orchestrator,
                      MarkdownReportGenerator markdownReportGenerator,
                      JsonReportWriter jsonReportWriter,
                      ConfigurableApplicationContext context) {
        this.orchestrator = orchestrator;
        this.markdownReportGenerator = markdownReportGenerator;
        this.jsonReportWriter = jsonReportWriter;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("input")) {
            return; // не CLI-режим — продолжаем запуск веб-приложения
        }

        int exitCode = execute(args);
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    private int execute(ApplicationArguments args) {
        String inputPath = firstOrNull(args.getOptionValues("input"));
        String outputPath = firstOrNull(args.getOptionValues("output"));
        String format = firstOrNull(args.getOptionValues("format"));
        if (format == null) {
            format = (outputPath != null && outputPath.toLowerCase().endsWith(".json")) ? "json" : "md";
        }
        if (outputPath == null) {
            outputPath = "reports/result." + (format.equals("json") ? "json" : "md");
        }

        Path input = Path.of(inputPath);
        if (!Files.exists(input)) {
            System.err.println("Ошибка: входной файл не найден: " + inputPath);
            return 1;
        }

        Path output = Path.of(outputPath);
        try {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("Ошибка: не удалось создать выходную директорию: " + e.getMessage());
            return 1;
        }

        System.out.println("=== Requirements Intelligence Agent ===");
        System.out.println("Входной файл: " + input.toAbsolutePath());
        System.out.println("Выходной файл: " + output.toAbsolutePath());
        System.out.println();

        try {
            AnalysisResult result = orchestrator.run(input, input.getFileName().toString(), stage ->
                    System.out.println("[" + java.time.LocalTime.now().withNano(0) + "] Этап: " + describeStage(stage)));

            String content = format.equalsIgnoreCase("json")
                    ? jsonReportWriter.toJson(result)
                    : markdownReportGenerator.generate(result);

            writeAtomically(output, content);

            System.out.println();
            System.out.println("Готово. Вердикт: " + result.getVerdict()
                    + " | Health Score: " + result.getHealthScore().getOverallScore() + "/100");
            System.out.println("Отчёт сохранён: " + output.toAbsolutePath());
            return 0;
        } catch (DocumentParsingException e) {
            System.err.println("Ошибка разбора документа: " + e.getMessage());
            return 2;
        } catch (LLMException e) {
            System.err.println("Ошибка обращения к LLM (" + e.kind() + "): " + e.getMessage());
            System.err.println("Анализ прерван — фиктивный результат не формируется.");
            return 3;
        } catch (Exception e) {
            System.err.println("Непредвиденная ошибка: " + e.getMessage());
            return 4;
        }
    }

    private String describeStage(AnalysisOrchestrator.Stage stage) {
        return switch (stage) {
            case PARSING -> "разбор документа";
            case EXTRACTING -> "извлечение требований";
            case ANALYZING_INDIVIDUAL -> "анализ отдельных требований";
            case ANALYZING_SET -> "анализ набора требований (противоречия, дубликаты)";
            case VERIFYING -> "проверка доказательств";
            case SCORING -> "расчёт Health Score и формирование отчёта";
            case DONE -> "готово";
        };
    }

    /** Пишем во временный файл и атомарно переименовываем — чтобы не оставить повреждённый отчёт. */
    private void writeAtomically(Path target, String content) throws IOException {
        Path tmp = Files.createTempFile(
                target.toAbsolutePath().getParent() != null ? target.toAbsolutePath().getParent() : Path.of("."),
                "report-", ".tmp");
        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private String firstOrNull(java.util.List<String> values) {
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
