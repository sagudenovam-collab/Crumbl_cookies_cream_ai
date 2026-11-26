package com.crumble_cookies.cream_ai.report;

import com.crumble_cookies.cream_ai.model.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportGeneratorTest {

    private final MarkdownReportGenerator generator = new MarkdownReportGenerator();

    @Test
    void generatesAllFifteenRequiredSections() {
        Requirement req = Requirement.builder().internalId("REQ-001").originalId("FR-1")
                .originalText("Система должна поддерживать вход по логину и паролю.")
                .summary("Вход по логину/паролю").source("demo.md")
                .location(new SourceLocation("demo.md", null, 5))
                .type(RequirementType.FUNCTIONAL).build();

        Finding finding = Finding.builder().id("IND-REQ-001-1").relatedRequirementIds(List.of("REQ-001"))
                .criterion("Полнота").quote("вход по логину и паролю")
                .location(req.getLocation()).explanation("не описано поведение при неверном пароле")
                .severity(Severity.MEDIUM).severityReason("не критично, но требует уточнения")
                .priority(Priority.P2).confidence(0.8).recommendation("добавить сценарий ошибки входа")
                .status(EvidenceStatus.CONFIRMED_ISSUE).verificationNote("цитата найдена").scope("INDIVIDUAL").build();

        AnalysisResult result = AnalysisResult.builder()
                .passport(AnalysisPassport.builder().documentName("demo.md").analyzedAt(OffsetDateTime.now())
                        .modelUsed("fake-model").agentVersion("1.0.0").pagesProcessed(null).requirementsExtracted(1).build())
                .executiveSummary(ExecutiveSummary.builder().overallState("ok").totalRequirements(1)
                        .confirmedIssues(1).potentialIssues(0).needsHumanReview(0).criticalDefects(0)
                        .healthScore(90).mainRisks(List.of("нет критичных рисков"))
                        .readiness(Verdict.READY_WITH_RESERVATIONS).topFixesFirst(List.of("уточнить сценарий ошибки")).build())
                .healthScore(HealthScoreBreakdown.builder().overallScore(90)
                        .byCriterion(Map.of("Полнота", 85)).severityDistribution(Map.of(Severity.MEDIUM, 1L))
                        .setLevelPenalty(0).evidenceConfidenceFactor(1.0)
                        .explanationNotes(List.of("пример пояснения")).build())
                .requirements(List.of(req))
                .individualFindings(List.of(finding))
                .setLevelFindings(List.of())
                .contradictions(List.of())
                .duplicates(List.of())
                .traceabilityMatrix(List.of(TraceabilityRow.builder().internalId("REQ-001").originalId("FR-1")
                        .relatedRequirementIds(List.of()).findingIds(List.of("IND-REQ-001-1"))
                        .violatedCriteria(List.of("Полнота")).recommendations(List.of("добавить сценарий ошибки")).build()))
                .verdict(Verdict.READY_WITH_RESERVATIONS)
                .verdictExplanation("Критичных дефектов нет, есть одно замечание.")
                .itemsNeedingHumanReview(List.of())
                .build();

        String markdown = generator.generate(result);

        assertThat(markdown).contains("# Отчёт об анализе качества требований");
        assertThat(markdown).contains("## 1. Executive Summary");
        assertThat(markdown).contains("## 2. Паспорт анализа");
        assertThat(markdown).contains("## 3. Requirement Health Score");
        assertThat(markdown).contains("## 4. Распределение проблем по серьёзности");
        assertThat(markdown).contains("## 5. Таблица извлечённых требований");
        assertThat(markdown).contains("## 6. Подробный анализ каждого требования");
        assertThat(markdown).contains("## 7. Анализ всего набора требований");
        assertThat(markdown).contains("## 8. Таблица противоречий");
        assertThat(markdown).contains("## 9. Таблица дубликатов");
        assertThat(markdown).contains("## 10. Матрица прослеживаемости");
        assertThat(markdown).contains("## 11. Рекомендации по улучшению");
        assertThat(markdown).contains("## 12. Примеры улучшенных формулировок");
        assertThat(markdown).contains("## 13. Приоритетный план исправлений");
        assertThat(markdown).contains("## 14. Итоговый вердикт");
        assertThat(markdown).contains("REQ-001");
        assertThat(markdown).contains("вход по логину и паролю");
    }
}
