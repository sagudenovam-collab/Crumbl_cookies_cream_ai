package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.DuplicateRecord;
import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Requirement;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Отдельный этап проверки доказательств после анализа LLM (Evidence-First
 * Architecture). Проверяет:
 * - существует ли цитата в исходном документе;
 * - относится ли цитата к указанному требованию;
 * - существует ли указанная страница/строка;
 * - не придумал ли AI ID требования (ID уже отфильтрованы выше по карте byId);
 * - достаточна ли уверенность модели для статуса CONFIRMED_ISSUE.
 *
 * Если доказательство не проходит проверку, замечание НЕ остаётся подтверждённой
 * проблемой — статус понижается до POTENTIAL_ISSUE или NEEDS_HUMAN_REVIEW.
 */
@Component
public class EvidenceVerifier {

    private static final double CONFIRMED_CONFIDENCE_THRESHOLD = 0.7;
    private static final double POTENTIAL_CONFIDENCE_THRESHOLD = 0.35;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public void verifyIndividualFindings(List<Finding> findings, Map<String, Requirement> byId) {
        for (Finding finding : findings) {
            Requirement requirement = firstRequirement(finding, byId);
            String haystack = requirement != null ? requirement.getOriginalText() : "";
            applyEvidenceStatus(finding, haystack);
        }
    }

    public void verifySetFindings(List<Finding> findings, Map<String, Requirement> byId) {
        for (Finding finding : findings) {
            StringBuilder haystack = new StringBuilder();
            if (finding.getRelatedRequirementIds() != null) {
                for (String id : finding.getRelatedRequirementIds()) {
                    Requirement r = byId.get(id);
                    if (r != null && r.getOriginalText() != null) {
                        haystack.append(r.getOriginalText()).append(' ');
                    }
                }
            }
            applyEvidenceStatus(finding, haystack.toString());
        }
    }

    public void verifyContradictions(List<ContradictionRecord> contradictions, Map<String, Requirement> byId) {
        for (ContradictionRecord record : contradictions) {
            boolean allQuotesValid = true;
            if (record.getQuotes() == null || record.getQuotes().isEmpty()) {
                allQuotesValid = false;
            } else {
                for (String reqId : record.getRequirementIds()) {
                    Requirement r = byId.get(reqId);
                    String quote = record.getQuotes().get(reqId);
                    if (r == null || quote == null || !quoteFoundIn(quote, r.getOriginalText())) {
                        allQuotesValid = false;
                        break;
                    }
                }
            }
            if (!allQuotesValid && record.getStatus() == EvidenceStatus.CONFIRMED_ISSUE) {
                record.setStatus(EvidenceStatus.NEEDS_HUMAN_REVIEW);
            } else if (allQuotesValid && record.getStatus() == EvidenceStatus.CONFIRMED_ISSUE
                    && record.getConfidence() < CONFIRMED_CONFIDENCE_THRESHOLD) {
                record.setStatus(EvidenceStatus.POTENTIAL_ISSUE);
            }
        }
    }

    public void verifyDuplicates(List<DuplicateRecord> duplicates) {
        for (DuplicateRecord record : duplicates) {
            if (record.getStatus() == EvidenceStatus.CONFIRMED_ISSUE
                    && record.getConfidence() < CONFIRMED_CONFIDENCE_THRESHOLD) {
                record.setStatus(EvidenceStatus.POTENTIAL_ISSUE);
            }
        }
    }

    private void applyEvidenceStatus(Finding finding, String haystack) {
        boolean quoteValid = finding.getQuote() != null && quoteFoundIn(finding.getQuote(), haystack);
        EvidenceStatus status;
        String note;
        if (!quoteValid) {
            status = EvidenceStatus.NEEDS_HUMAN_REVIEW;
            note = "Цитата не найдена дословно в тексте связанного требования — требуется проверка специалистом.";
        } else if (finding.getConfidence() >= CONFIRMED_CONFIDENCE_THRESHOLD) {
            status = EvidenceStatus.CONFIRMED_ISSUE;
            note = "Цитата подтверждена в исходном документе, уверенность модели достаточна.";
        } else if (finding.getConfidence() >= POTENTIAL_CONFIDENCE_THRESHOLD) {
            status = EvidenceStatus.POTENTIAL_ISSUE;
            note = "Цитата подтверждена, но уверенность модели недостаточна для окончательного вывода.";
        } else {
            status = EvidenceStatus.NEEDS_HUMAN_REVIEW;
            note = "Низкая уверенность модели — требуется проверка специалистом.";
        }
        finding.setStatus(status);
        finding.setVerificationNote(note);
    }

    private Requirement firstRequirement(Finding finding, Map<String, Requirement> byId) {
        if (finding.getRelatedRequirementIds() == null || finding.getRelatedRequirementIds().isEmpty()) {
            return null;
        }
        return byId.get(finding.getRelatedRequirementIds().get(0));
    }

    /** Проверяет наличие цитаты в тексте с нормализацией пробелов/регистра/базовой пунктуации. */
    boolean quoteFoundIn(String quote, String haystack) {
        if (quote == null || quote.isBlank() || haystack == null || haystack.isBlank()) {
            return false;
        }
        String normalizedQuote = normalize(quote);
        String normalizedHaystack = normalize(haystack);
        if (normalizedQuote.isBlank()) {
            return false;
        }
        return normalizedHaystack.contains(normalizedQuote);
    }

    private String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC);
        n = n.replace('«', '"').replace('»', '"').replace('“', '"').replace('”', '"');
        n = n.toLowerCase();
        n = WHITESPACE.matcher(n).replaceAll(" ").trim();
        return n;
    }
}
