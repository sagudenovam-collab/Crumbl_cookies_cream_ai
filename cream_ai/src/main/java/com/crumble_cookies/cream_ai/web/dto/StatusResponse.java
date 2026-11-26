package com.crumble_cookies.cream_ai.web.dto;

import com.crumble_cookies.cream_ai.web.AnalysisJob;

import java.time.OffsetDateTime;

public record StatusResponse(
        String analysisId,
        String fileName,
        AnalysisJob.Status status,
        OffsetDateTime createdAt,
        String errorKind,
        String errorMessage
) {
    public static StatusResponse from(AnalysisJob job) {
        return new StatusResponse(job.getId(), job.getFileName(), job.getStatus(), job.getCreatedAt(),
                job.getErrorKind(), job.getErrorMessage());
    }
}
