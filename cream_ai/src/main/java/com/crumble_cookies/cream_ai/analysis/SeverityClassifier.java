package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Priority;
import com.crumble_cookies.cream_ai.model.Severity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Назначает приоритет исправления на основе серьёзности и не позволяет
 * завышать серьёзность для замечаний, которые не прошли проверку доказательств.
 */
@Component
public class SeverityClassifier {

    public void classify(List<Finding> findings) {
        for (Finding finding : findings) {
            capSeverityForUnverifiedEvidence(finding);
            finding.setPriority(toPriority(finding.getSeverity()));
        }
    }

    private void capSeverityForUnverifiedEvidence(Finding finding) {
        if (finding.getStatus() == EvidenceStatus.NEEDS_HUMAN_REVIEW
                && (finding.getSeverity() == Severity.CRITICAL || finding.getSeverity() == Severity.HIGH)) {
            finding.setSeverity(Severity.MEDIUM);
            String note = finding.getSeverityReason() == null ? "" : finding.getSeverityReason() + " ";
            finding.setSeverityReason(note
                    + "Серьёзность ограничена до MEDIUM: доказательство не прошло проверку "
                    + "(не следует завышать серьёзность непроверенного замечания).");
        }
    }

    private Priority toPriority(Severity severity) {
        return switch (severity) {
            case CRITICAL -> Priority.P0;
            case HIGH -> Priority.P1;
            case MEDIUM -> Priority.P2;
            case LOW -> Priority.P3;
        };
    }
}
