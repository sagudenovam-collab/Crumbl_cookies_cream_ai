package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Отдельное извлечённое требование. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Requirement {

    /** Внутренний ID, например REQ-001. Всегда присваивается. */
    private String internalId;

    /** Исходный ID из документа, если присутствует. Иначе null. */
    private String originalId;

    /** Точный оригинальный текст требования. */
    private String originalText;

    /** Краткое описание (1 предложение). */
    private String summary;

    /** Источник (имя файла). */
    private String source;

    /** Местоположение в документе. */
    private SourceLocation location;

    /** Тип требования, если удалось определить. */
    private RequirementType type;

    /** Фрагмент неоднозначен — требуется проверка человеком. */
    private boolean needsHumanReview;

    /** Причина, по которой требуется проверка человеком. */
    private String humanReviewReason;
}
