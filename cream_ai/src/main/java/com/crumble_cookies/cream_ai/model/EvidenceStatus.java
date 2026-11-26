package com.crumble_cookies.cream_ai.model;

/**
 * Статус доказательности замечания (Evidence-First Architecture).
 * Уверенность AI не равна доказанности нарушения.
 */
public enum EvidenceStatus {
    /** Проблема подтверждается непосредственно содержанием документа. */
    CONFIRMED_ISSUE,
    /** Вероятная проблема, но недостаточно контекста для окончательного вывода. */
    POTENTIAL_ISSUE,
    /** Требуется решение специалиста. */
    NEEDS_HUMAN_REVIEW
}
