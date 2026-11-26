package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Паспорт анализа — метаданные прогона. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisPassport {
    private String documentName;
    private OffsetDateTime analyzedAt;
    private String modelUsed;
    private String agentVersion;
    private String gitCommit;
    private Integer pagesProcessed;
    private int requirementsExtracted;
}
