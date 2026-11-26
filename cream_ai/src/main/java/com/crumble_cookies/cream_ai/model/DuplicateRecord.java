package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Пара или группа дублирующихся требований. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateRecord {
    private String id;
    private List<String> requirementIds;
    private String explanation;
    private EvidenceStatus status;
    private double confidence;
}
