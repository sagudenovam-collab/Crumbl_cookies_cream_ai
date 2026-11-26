package com.crumble_cookies.cream_ai;

import com.crumble_cookies.cream_ai.analysis.*;
import com.crumble_cookies.cream_ai.extraction.RequirementExtractor;
import com.crumble_cookies.cream_ai.extraction.RequirementNormalizer;
import com.crumble_cookies.cream_ai.model.AnalysisResult;
import com.crumble_cookies.cream_ai.model.Verdict;
import com.crumble_cookies.cream_ai.parser.DocumentParserRegistry;
import com.crumble_cookies.cream_ai.parser.DocxDocumentParser;
import com.crumble_cookies.cream_ai.parser.MarkdownDocumentParser;
import com.crumble_cookies.cream_ai.parser.PdfDocumentParser;
import com.crumble_cookies.cream_ai.parser.PlainTextDocumentParser;
import com.crumble_cookies.cream_ai.report.MarkdownReportGenerator;
import com.crumble_cookies.cream_ai.support.FakeLLMProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Полный end-to-end тест конвейера: от демонстрационного Markdown-документа
 * (с намеренно заложенными дефектами) до готового AnalysisResult и Markdown-отчёта,
 * без обращения к реальному Gemini API (используется FakeLLMProvider).
 */
class AnalysisOrchestratorE2ETest {

    @Test
    void runsFullPipelineOnDemoDocumentWithSeededDefects(@TempDir Path dir) throws Exception {
        Path doc = dir.resolve("demo.md");
        Files.writeString(doc, """
                # Demo Document

                REQ-1: Система должна разрешать пользователю войти по логину и паролю.

                REQ-2: Система должна работать быстро.

                REQ-3: Время отклика API должно быть не более 2 секунд.

                REQ-4: Время отклика API должно быть не более 10 секунд.

                REQ-5: Пользователь может выйти из системы.

                REQ-6: Пользователь может завершить сеанс в приложении.
                """);

        FakeLLMProvider fake = new FakeLLMProvider();

        // 1) извлечение требований (пропускаем заголовок, index 0)
        fake.queueResponse("""
                [
                  {"originalId":"REQ-1","text":"Система должна разрешать пользователю войти по логину и паролю.","summary":"Вход по логину/паролю","type":"FUNCTIONAL","segmentRefs":[1],"ambiguous":false,"ambiguousReason":null,"isActualRequirement":true},
                  {"originalId":"REQ-2","text":"Система должна работать быстро.","summary":"Быстрая работа","type":"NON_FUNCTIONAL","segmentRefs":[2],"ambiguous":true,"ambiguousReason":"нет измеримого критерия быстроты","isActualRequirement":true},
                  {"originalId":"REQ-3","text":"Время отклика API должно быть не более 2 секунд.","summary":"Отклик API 2с","type":"NON_FUNCTIONAL","segmentRefs":[3],"ambiguous":false,"ambiguousReason":null,"isActualRequirement":true},
                  {"originalId":"REQ-4","text":"Время отклика API должно быть не более 10 секунд.","summary":"Отклик API 10с","type":"NON_FUNCTIONAL","segmentRefs":[4],"ambiguous":false,"ambiguousReason":null,"isActualRequirement":true},
                  {"originalId":"REQ-5","text":"Пользователь может выйти из системы.","summary":"Выход из системы","type":"FUNCTIONAL","segmentRefs":[5],"ambiguous":false,"ambiguousReason":null,"isActualRequirement":true},
                  {"originalId":"REQ-6","text":"Пользователь может завершить сеанс в приложении.","summary":"Завершение сеанса","type":"FUNCTIONAL","segmentRefs":[6],"ambiguous":false,"ambiguousReason":null,"isActualRequirement":true}
                ]
                """);

        // 2) индивидуальный анализ (REQ-002 соответствует исходному REQ-2 "быстро")
        fake.queueResponse("""
                {"results": [
                  {"requirementId":"REQ-001","qualityOk":true,"findings":[]},
                  {"requirementId":"REQ-002","qualityOk":false,"findings":[
                     {"criterion":"Проверяемость","quote":"работать быстро","explanation":"нет измеримого критерия быстроты",
                      "severity":"MEDIUM","severityReason":"снижает проверяемость, но не блокирует разработку",
                      "confidence":0.85,"recommendation":"указать конкретное измеримое время отклика",
                      "improvedWording":"Система должна отвечать не более чем за [ЗНАЧЕНИЕ ОПРЕДЕЛЯЕТ ЗАКАЗЧИК] секунд."}
                  ]},
                  {"requirementId":"REQ-003","qualityOk":true,"findings":[]},
                  {"requirementId":"REQ-004","qualityOk":true,"findings":[]},
                  {"requirementId":"REQ-005","qualityOk":true,"findings":[]},
                  {"requirementId":"REQ-006","qualityOk":true,"findings":[]}
                ]}
                """);

        // 3) анализ набора: противоречие REQ-003 vs REQ-004, дубликат REQ-005 vs REQ-006
        fake.queueResponse("""
                {
                  "contradictions": [
                    {"requirementIds":["REQ-003","REQ-004"],
                     "quotes":{"REQ-003":"не более 2 секунд","REQ-004":"не более 10 секунд"},
                     "explanation":"два требования задают разное время отклика для API",
                     "possibleConsequence":"непонятно, какой SLA реализовывать",
                     "resolutionSuggestion":"уточнить у заказчика единое значение",
                     "status":"CONFIRMED_ISSUE","confidence":0.9}
                  ],
                  "duplicates": [
                    {"requirementIds":["REQ-005","REQ-006"],
                     "explanation":"оба требования описывают выход пользователя из системы",
                     "status":"CONFIRMED_ISSUE","confidence":0.8}
                  ],
                  "other": []
                }
                """);

        DocumentParserRegistry registry = new DocumentParserRegistry(java.util.List.of(
                new PdfDocumentParser(), new MarkdownDocumentParser(), new DocxDocumentParser(), new PlainTextDocumentParser()));

        AnalysisOrchestrator orchestrator = new AnalysisOrchestrator(
                registry,
                new RequirementExtractor(fake),
                new RequirementNormalizer(),
                new IndividualRequirementAnalyzer(fake),
                new RequirementSetAnalyzer(fake),
                new ContradictionDetector(),
                new EvidenceVerifier(),
                new SeverityClassifier(),
                new RequirementScorer(),
                new TraceabilityAnalyzer(),
                new VerdictCalculator(),
                fake
        );

        AnalysisResult result = orchestrator.run(doc, "demo.md");

        assertThat(result.getRequirements()).hasSize(6);
        assertThat(result.getContradictions()).hasSize(1);
        assertThat(result.getContradictions().get(0).getRequirementIds()).containsExactly("REQ-003", "REQ-004");
        assertThat(result.getDuplicates()).hasSize(1);
        assertThat(result.getIndividualFindings()).anySatisfy(f -> assertThat(f.getQuote()).contains("быстро"));
        assertThat(result.getVerdict()).isIn(Verdict.READY_WITH_RESERVATIONS, Verdict.NOT_READY);
        assertThat(result.getHealthScore().getOverallScore()).isBetween(0, 100);

        // отчёт должен успешно сформироваться и содержать ключевые находки
        String markdown = new MarkdownReportGenerator().generate(result);
        assertThat(markdown).contains("REQ-003").contains("REQ-004").contains("REQ-005").contains("REQ-006");
    }
}
