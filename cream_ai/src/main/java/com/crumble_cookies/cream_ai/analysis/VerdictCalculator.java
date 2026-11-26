package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.DuplicateRecord;
import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.Severity;
import com.crumble_cookies.cream_ai.model.Verdict;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Рассчитывает итоговый вердикт готовности документа. Вывод о готовности НЕ
 * делается только на основании среднего балла — подтверждённые критичные
 * дефекты имеют отдельное, решающее влияние на вердикт.
 */
@Component
public class VerdictCalculator {

    public record Result(Verdict verdict, String explanation) {
    }

    public Result calculate(List<Requirement> requirements, List<Finding> allFindings,
                             List<ContradictionRecord> contradictions, List<DuplicateRecord> duplicates,
                             int healthScore) {
        if (requirements.isEmpty()) {
            return new Result(Verdict.INSUFFICIENT_INFORMATION,
                    "В документе не найдено ни одного требования, пригодного для анализа. "
                            + "Недостаточно информации для вывода о готовности.");
        }

        long confirmedCritical = countConfirmed(allFindings, contradictions, duplicates, Severity.CRITICAL);
        long confirmedHigh = countConfirmed(allFindings, contradictions, duplicates, Severity.HIGH);

        long potentialOrReview = allFindings.stream()
                .filter(f -> f.getStatus() == EvidenceStatus.POTENTIAL_ISSUE || f.getStatus() == EvidenceStatus.NEEDS_HUMAN_REVIEW)
                .count();
        long needsHumanReviewRequirements = requirements.stream().filter(Requirement::isNeedsHumanReview).count();

        double uncertaintyRatio = requirements.isEmpty() ? 0
                : (double) (potentialOrReview + needsHumanReviewRequirements) / requirements.size();

        if (confirmedCritical > 0) {
            return new Result(Verdict.NOT_READY,
                    "Обнаружено подтверждённых критичных дефектов: " + confirmedCritical
                            + ". Документ не готов к следующему этапу без их устранения.");
        }
        if (confirmedHigh >= 3 || (double) confirmedHigh / requirements.size() > 0.2) {
            return new Result(Verdict.NOT_READY,
                    "Обнаружено значительное количество подтверждённых серьёзных дефектов (HIGH): "
                            + confirmedHigh + ". Требуется доработка перед следующим этапом.");
        }
        if (uncertaintyRatio > 0.5) {
            return new Result(Verdict.INSUFFICIENT_INFORMATION,
                    "Более половины требований/замечаний помечены как требующие проверки человеком "
                            + "или являются лишь предположением — недостаточно подтверждённой информации "
                            + "для надёжного вывода о готовности.");
        }
        if (confirmedHigh > 0 || potentialOrReview > 0) {
            return new Result(Verdict.READY_WITH_RESERVATIONS,
                    "Критичных подтверждённых дефектов не найдено, но есть замечания (включая " + confirmedHigh
                            + " подтверждённых HIGH), которые стоит учесть перед разработкой.");
        }
        return new Result(Verdict.READY,
                "Критичных и серьёзных подтверждённых дефектов не обнаружено. Health Score: " + healthScore + "/100.");
    }

    private long countConfirmed(List<Finding> findings, List<ContradictionRecord> contradictions,
                                 List<DuplicateRecord> duplicates, Severity severity) {
        long findingCount = findings.stream()
                .filter(f -> f.getStatus() == EvidenceStatus.CONFIRMED_ISSUE && f.getSeverity() == severity)
                .count();
        // Противоречия без явной severity считаем эквивалентом HIGH по умолчанию для учёта в вердикте,
        // т.к. противоречие между требованиями по природе является серьёзным системным дефектом.
        long contradictionCount = severity == Severity.HIGH
                ? contradictions.stream().filter(c -> c.getStatus() == EvidenceStatus.CONFIRMED_ISSUE).count()
                : 0;
        return findingCount + contradictionCount;
    }
}
