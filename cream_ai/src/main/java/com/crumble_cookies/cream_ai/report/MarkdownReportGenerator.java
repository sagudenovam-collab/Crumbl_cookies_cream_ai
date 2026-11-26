package com.crumble_cookies.cream_ai.report;

import com.crumble_cookies.cream_ai.model.AnalysisPassport;
import com.crumble_cookies.cream_ai.model.AnalysisResult;
import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.DuplicateRecord;
import com.crumble_cookies.cream_ai.model.ExecutiveSummary;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.HealthScoreBreakdown;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.Severity;
import com.crumble_cookies.cream_ai.model.TraceabilityRow;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Генерирует итоговый отчёт на русском языке в формате Markdown согласно
 * обязательной структуре из технического задания (15 разделов).
 */
@Component
public class MarkdownReportGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public String generate(AnalysisResult r) {
        StringBuilder md = new StringBuilder();
        section1Title(md, r);
        section2ExecutiveSummary(md, r.getExecutiveSummary());
        section3Passport(md, r.getPassport());
        section4HealthScore(md, r.getHealthScore());
        section5SeverityDistribution(md, r.getHealthScore());
        section6RequirementsTable(md, r.getRequirements());
        section7DetailedAnalysis(md, r.getRequirements(), r.getIndividualFindings());
        section8SetAnalysis(md, r.getSetLevelFindings());
        section9Contradictions(md, r.getContradictions());
        section10Duplicates(md, r.getDuplicates());
        section11Traceability(md, r.getTraceabilityMatrix());
        section12Recommendations(md, r);
        section13ImprovedWording(md, r.getIndividualFindings());
        section14PriorityPlan(md, r);
        section15FinalVerdict(md, r);
        return md.toString();
    }

    private void section1Title(StringBuilder md, AnalysisResult r) {
        md.append("# Отчёт об анализе качества требований\n\n");
        md.append("**Документ:** ").append(nullSafe(r.getPassport().getDocumentName())).append("  \n");
        md.append("**Дата анализа:** ").append(r.getPassport().getAnalyzedAt().format(DATE_FORMAT)).append("  \n");
        md.append("**Агент:** Requirements Intelligence Agent v").append(r.getPassport().getAgentVersion()).append("  \n\n");
        md.append("---\n\n");
    }

    private void section2ExecutiveSummary(StringBuilder md, ExecutiveSummary s) {
        md.append("## 1. Executive Summary\n\n");
        md.append(s.getOverallState()).append("\n\n");
        md.append("| Показатель | Значение |\n|---|---|\n");
        md.append("| Всего требований | ").append(s.getTotalRequirements()).append(" |\n");
        md.append("| Подтверждённых дефектов (CONFIRMED_ISSUE) | ").append(s.getConfirmedIssues()).append(" |\n");
        md.append("| Потенциальных проблем (POTENTIAL_ISSUE) | ").append(s.getPotentialIssues()).append(" |\n");
        md.append("| Требуют проверки человеком | ").append(s.getNeedsHumanReview()).append(" |\n");
        md.append("| Критичных дефектов | ").append(s.getCriticalDefects()).append(" |\n");
        md.append("| Requirement Health Score | ").append(s.getHealthScore()).append("/100 |\n");
        md.append("| Готовность к разработке | ").append(verdictLabel(s.getReadiness())).append(" |\n\n");

        md.append("**Главные риски:**\n\n");
        for (String risk : s.getMainRisks()) {
            md.append("- ").append(risk).append('\n');
        }
        md.append("\n**Что исправить в первую очередь:**\n\n");
        for (String fix : s.getTopFixesFirst()) {
            md.append("- ").append(fix).append('\n');
        }
        md.append("\n---\n\n");
    }

    private void section3Passport(StringBuilder md, AnalysisPassport p) {
        md.append("## 2. Паспорт анализа\n\n");
        md.append("| Поле | Значение |\n|---|---|\n");
        md.append("| Название исходного документа | ").append(nullSafe(p.getDocumentName())).append(" |\n");
        md.append("| Дата и время анализа | ").append(p.getAnalyzedAt().format(DATE_FORMAT)).append(" |\n");
        md.append("| Используемая модель | ").append(nullSafe(p.getModelUsed())).append(" |\n");
        md.append("| Версия агента | ").append(nullSafe(p.getAgentVersion())).append(" |\n");
        md.append("| Git commit | ").append(p.getGitCommit() != null ? p.getGitCommit() : "недоступен") .append(" |\n");
        md.append("| Количество обработанных страниц | ")
                .append(p.getPagesProcessed() != null ? p.getPagesProcessed() : "н/д (формат без постраничной разбивки)")
                .append(" |\n");
        md.append("| Количество извлечённых требований | ").append(p.getRequirementsExtracted()).append(" |\n\n");
        md.append("---\n\n");
    }

    private void section4HealthScore(StringBuilder md, HealthScoreBreakdown h) {
        md.append("## 3. Requirement Health Score\n\n");
        md.append("**Итоговый балл: ").append(h.getOverallScore()).append("/100**\n\n");
        md.append("> Это собственная прозрачная методика продукта, а не официальная оценка ISO.\n\n");
        md.append("### Баллы по критериям ISO/IEC/IEEE 29148 и IEEE 830\n\n");
        md.append("| Критерий | Балл |\n|---|---|\n");
        for (Map.Entry<String, Integer> e : h.getByCriterion().entrySet()) {
            md.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append("/100 |\n");
        }
        md.append("\n**Штраф за системные проблемы набора:** -").append(h.getSetLevelPenalty()).append("\n\n");
        md.append("**Коэффициент доказанности замечаний:** ").append(h.getEvidenceConfidenceFactor()).append("\n\n");
        md.append("### Из чего складывается балл\n\n");
        for (String note : h.getExplanationNotes()) {
            md.append("- ").append(note).append('\n');
        }
        md.append("\n---\n\n");
    }

    private void section5SeverityDistribution(StringBuilder md, HealthScoreBreakdown h) {
        md.append("## 4. Распределение проблем по серьёзности\n\n");
        md.append("| Серьёзность | Количество |\n|---|---|\n");
        for (Severity s : Severity.values()) {
            md.append("| ").append(s).append(" | ").append(h.getSeverityDistribution().getOrDefault(s, 0L)).append(" |\n");
        }
        md.append("\n---\n\n");
    }

    private void section6RequirementsTable(StringBuilder md, List<Requirement> requirements) {
        md.append("## 5. Таблица извлечённых требований\n\n");
        if (requirements.isEmpty()) {
            md.append("_Требования не были извлечены из документа._\n\n---\n\n");
            return;
        }
        md.append("| Внутренний ID | Исходный ID | Краткое описание | Источник | Местоположение |\n");
        md.append("|---|---|---|---|---|\n");
        for (Requirement r : requirements) {
            md.append("| ").append(r.getInternalId())
                    .append(" | ").append(nullSafe(r.getOriginalId()))
                    .append(" | ").append(escapeTable(r.getSummary()))
                    .append(" | ").append(nullSafe(r.getSource()))
                    .append(" | ").append(r.getLocation() != null ? r.getLocation().describe() : "н/д")
                    .append(" |\n");
        }
        md.append("\n---\n\n");
    }

    private void section7DetailedAnalysis(StringBuilder md, List<Requirement> requirements, List<Finding> findings) {
        md.append("## 6. Подробный анализ каждого требования\n\n");
        for (Requirement r : requirements) {
            md.append("### ").append(r.getInternalId());
            if (r.getOriginalId() != null) {
                md.append(" (исходный ID: ").append(r.getOriginalId()).append(")");
            }
            md.append("\n\n");
            md.append("> ").append(r.getOriginalText()).append("\n\n");
            md.append("- **Тип:** ").append(r.getType()).append("\n");
            md.append("- **Источник:** ").append(nullSafe(r.getSource()))
                    .append(" (").append(r.getLocation() != null ? r.getLocation().describe() : "н/д").append(")\n");
            if (r.isNeedsHumanReview()) {
                md.append("- **Требуется проверка человеком:** ").append(nullSafe(r.getHumanReviewReason())).append("\n");
            }

            List<Finding> own = findings.stream()
                    .filter(f -> f.getRelatedRequirementIds().contains(r.getInternalId()))
                    .toList();

            if (own.isEmpty()) {
                md.append("\n✅ Замечаний не найдено — требование соответствует проверенным критериям.\n\n");
            } else {
                md.append("\n**Найденные замечания:**\n\n");
                for (Finding f : own) {
                    appendFinding(md, f);
                }
            }
            md.append("---\n\n");
        }
    }

    private void appendFinding(StringBuilder md, Finding f) {
        md.append("- **[").append(f.getStatus()).append(" / ").append(f.getSeverity())
                .append(" / ").append(f.getPriority()).append("] ").append(f.getCriterion()).append("**\n");
        md.append("  - Цитата: \"").append(f.getQuote()).append("\"\n");
        md.append("  - Объяснение: ").append(f.getExplanation()).append('\n');
        md.append("  - Почему такая серьёзность: ").append(nullSafe(f.getSeverityReason())).append('\n');
        md.append("  - Уверенность модели: ").append(String.format("%.0f%%", f.getConfidence() * 100)).append('\n');
        md.append("  - Рекомендация: ").append(f.getRecommendation()).append('\n');
        if (f.getImprovedWording() != null && !f.getImprovedWording().isBlank()) {
            md.append("  - Пример улучшенной формулировки: ").append(f.getImprovedWording()).append('\n');
        }
        if (f.getVerificationNote() != null) {
            md.append("  - Примечание верификации: ").append(f.getVerificationNote()).append('\n');
        }
    }

    private void section8SetAnalysis(StringBuilder md, List<Finding> setFindings) {
        md.append("## 7. Анализ всего набора требований\n\n");
        if (setFindings.isEmpty()) {
            md.append("_Системных замечаний уровня набора (помимо противоречий и дубликатов ниже) не обнаружено._\n\n");
        } else {
            for (Finding f : setFindings) {
                md.append("- **[").append(f.getStatus()).append(" / ").append(f.getSeverity()).append("] ")
                        .append(f.getCriterion()).append("** (").append(String.join(", ", f.getRelatedRequirementIds())).append(")\n");
                md.append("  - Цитата: \"").append(nullSafe(f.getQuote())).append("\"\n");
                md.append("  - Объяснение: ").append(f.getExplanation()).append('\n');
                md.append("  - Рекомендация: ").append(f.getRecommendation()).append('\n');
            }
        }
        md.append("\n---\n\n");
    }

    private void section9Contradictions(StringBuilder md, List<ContradictionRecord> contradictions) {
        md.append("## 8. Таблица противоречий (Contradiction Detective)\n\n");
        if (contradictions.isEmpty()) {
            md.append("_Противоречий между требованиями не обнаружено._\n\n---\n\n");
            return;
        }
        md.append("| ID | Требования | Статус | Уверенность | Объяснение | Возможные последствия | Как разрешить |\n");
        md.append("|---|---|---|---|---|---|---|\n");
        for (ContradictionRecord c : contradictions) {
            md.append("| ").append(c.getId())
                    .append(" | ").append(String.join(", ", c.getRequirementIds()))
                    .append(" | ").append(c.getStatus())
                    .append(" | ").append(String.format("%.0f%%", c.getConfidence() * 100))
                    .append(" | ").append(escapeTable(c.getExplanation()))
                    .append(" | ").append(escapeTable(c.getPossibleConsequence()))
                    .append(" | ").append(escapeTable(c.getResolutionSuggestion()))
                    .append(" |\n");
        }
        md.append("\n**Цитаты:**\n\n");
        for (ContradictionRecord c : contradictions) {
            md.append("- ").append(c.getId()).append(":\n");
            if (c.getQuotes() != null) {
                c.getQuotes().forEach((id, quote) -> md.append("  - ").append(id).append(": \"").append(quote).append("\"\n"));
            }
        }
        md.append("\n---\n\n");
    }

    private void section10Duplicates(StringBuilder md, List<DuplicateRecord> duplicates) {
        md.append("## 9. Таблица дубликатов\n\n");
        if (duplicates.isEmpty()) {
            md.append("_Дублирующихся требований не обнаружено._\n\n---\n\n");
            return;
        }
        md.append("| ID | Требования | Статус | Уверенность | Объяснение |\n|---|---|---|---|---|\n");
        for (DuplicateRecord d : duplicates) {
            md.append("| ").append(d.getId())
                    .append(" | ").append(String.join(", ", d.getRequirementIds()))
                    .append(" | ").append(d.getStatus())
                    .append(" | ").append(String.format("%.0f%%", d.getConfidence() * 100))
                    .append(" | ").append(escapeTable(d.getExplanation()))
                    .append(" |\n");
        }
        md.append("\n---\n\n");
    }

    private void section11Traceability(StringBuilder md, List<TraceabilityRow> rows) {
        md.append("## 10. Матрица прослеживаемости\n\n");
        if (rows.isEmpty()) {
            md.append("_Нет данных для построения матрицы прослеживаемости._\n\n---\n\n");
            return;
        }
        md.append("| ID | Исходный ID | Связанные требования | Замечания | Нарушенные критерии |\n|---|---|---|---|---|\n");
        for (TraceabilityRow row : rows) {
            md.append("| ").append(row.getInternalId())
                    .append(" | ").append(nullSafe(row.getOriginalId()))
                    .append(" | ").append(row.getRelatedRequirementIds().isEmpty() ? "—" : String.join(", ", row.getRelatedRequirementIds()))
                    .append(" | ").append(row.getFindingIds().isEmpty() ? "—" : String.join(", ", row.getFindingIds()))
                    .append(" | ").append(row.getViolatedCriteria().isEmpty() ? "—" : String.join(", ", row.getViolatedCriteria()))
                    .append(" |\n");
        }
        md.append("\n---\n\n");
    }

    private void section12Recommendations(StringBuilder md, AnalysisResult r) {
        md.append("## 11. Рекомендации по улучшению\n\n");
        List<Finding> all = new java.util.ArrayList<>(r.getIndividualFindings());
        all.addAll(r.getSetLevelFindings());
        all.stream()
                .filter(f -> f.getRecommendation() != null && !f.getRecommendation().isBlank())
                .sorted(Comparator.comparing(Finding::getSeverity))
                .limit(30)
                .forEach(f -> md.append("- (").append(String.join(", ", f.getRelatedRequirementIds())).append(") ")
                        .append(f.getRecommendation()).append('\n'));
        md.append("\n---\n\n");
    }

    private void section13ImprovedWording(StringBuilder md, List<Finding> individualFindings) {
        md.append("## 12. Примеры улучшенных формулировок (Smart Rewrite)\n\n");
        List<Finding> withWording = individualFindings.stream()
                .filter(f -> f.getImprovedWording() != null && !f.getImprovedWording().isBlank())
                .toList();
        if (withWording.isEmpty()) {
            md.append("_Для данного документа предложений по улучшению формулировок не потребовалось либо их "
                    + "нельзя дать без выдумывания бизнес-правил._\n\n---\n\n");
            return;
        }
        for (Finding f : withWording) {
            md.append("- **").append(String.join(", ", f.getRelatedRequirementIds())).append("**: ")
                    .append(f.getImprovedWording()).append('\n');
        }
        md.append("\n---\n\n");
    }

    private void section14PriorityPlan(StringBuilder md, AnalysisResult r) {
        md.append("## 13. Приоритетный план исправлений\n\n");
        List<Finding> all = new java.util.ArrayList<>(r.getIndividualFindings());
        all.addAll(r.getSetLevelFindings());
        md.append("| Приоритет | Требования | Серьёзность | Рекомендация |\n|---|---|---|---|\n");
        all.stream()
                .sorted(Comparator.comparing(Finding::getPriority))
                .forEach(f -> md.append("| ").append(f.getPriority())
                        .append(" | ").append(String.join(", ", f.getRelatedRequirementIds()))
                        .append(" | ").append(f.getSeverity())
                        .append(" | ").append(escapeTable(f.getRecommendation()))
                        .append(" |\n"));
        md.append("\n- **P0** — исправить до начала разработки.\n");
        md.append("- **P1** — исправить до реализации соответствующего функционала.\n");
        md.append("- **P2** — желательно исправить в ближайшей итерации.\n");
        md.append("- **P3** — улучшение качества документа.\n\n---\n\n");
    }

    private void section15FinalVerdict(StringBuilder md, AnalysisResult r) {
        md.append("## 14. Итоговый вердикт\n\n");
        md.append("### ").append(verdictLabel(r.getVerdict())).append('\n');
        md.append(r.getVerdictExplanation()).append("\n\n");
        if (!r.getItemsNeedingHumanReview().isEmpty()) {
            md.append("### Требуется проверка специалистом\n\n");
            for (String item : r.getItemsNeedingHumanReview()) {
                md.append("- ").append(item).append('\n');
            }
            md.append('\n');
        }
        md.append("---\n\n_Отчёт сформирован автоматически агентом Requirements Intelligence Agent._\n");
    }

    private String verdictLabel(com.crumble_cookies.cream_ai.model.Verdict v) {
        return switch (v) {
            case READY -> "READY — документ достаточно качественный для следующего этапа";
            case READY_WITH_RESERVATIONS -> "READY_WITH_RESERVATIONS — можно продолжать, но необходимо учесть замечания";
            case NOT_READY -> "NOT_READY — имеются проблемы, которые необходимо устранить";
            case INSUFFICIENT_INFORMATION -> "INSUFFICIENT_INFORMATION — недостаточно информации для надёжного вывода";
        };
    }

    private String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String escapeTable(String s) {
        if (s == null) return "—";
        return s.replace("\n", " ").replace("|", "\\|");
    }
}
