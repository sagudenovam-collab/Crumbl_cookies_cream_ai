package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Единичное замечание (дефект), найденное при анализе одного требования
 * или всего набора требований. Следует принципу Evidence First: каждое
 * замечание обязано содержать цитату, местоположение и статус доказательности.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Finding {

    private String id;

    /** ID требования или связанных требований. */
    private List<String> relatedRequirementIds;

    /** Какой критерий качества нарушен (например, "Однозначность"). */
    private String criterion;

    /** Точная цитата из исходного документа. */
    private String quote;

    /** Местоположение цитаты. */
    private SourceLocation location;

    /** Объяснение, почему это проблема. */
    private String explanation;

    private Severity severity;

    /** Почему выбран именно такой уровень серьёзности. */
    private String severityReason;

    private Priority priority;

    /** Уверенность модели, 0.0 - 1.0. */
    private double confidence;

    /** Рекомендация по исправлению. */
    private String recommendation;

    /** Пример улучшенной формулировки, если возможно без выдумывания бизнес-правил. */
    private String improvedWording;

    /** Статус доказательности после верификации. */
    private EvidenceStatus status;

    /** Объяснение, почему статус был понижен/подтверждён верификатором (если применимо). */
    private String verificationNote;

    /** INDIVIDUAL (отдельное требование) или SET (анализ набора). */
    private String scope;
}
