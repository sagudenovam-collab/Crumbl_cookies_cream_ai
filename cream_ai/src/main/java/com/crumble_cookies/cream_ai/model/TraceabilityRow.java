package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Строка матрицы прослеживаемости. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceabilityRow {
    private String internalId;
    private String originalId;
    private List<String> relatedRequirementIds;
    private List<String> findingIds;
    private List<String> violatedCriteria;
    private List<String> recommendations;
}
