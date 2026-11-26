package com.crumble_cookies.cream_ai.web;

import com.crumble_cookies.cream_ai.analysis.AnalysisOrchestrator;
import com.crumble_cookies.cream_ai.model.AnalysisResult;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

/** Состояние одного асинхронного прогона анализа, хранимое в памяти. */
public class AnalysisJob {

    public enum Status {
        PENDING, PARSING, EXTRACTING, ANALYZING_INDIVIDUAL, ANALYZING_SET, VERIFYING, SCORING, DONE, FAILED
    }

    private final String id;
    private final String fileName;
    private final OffsetDateTime createdAt = OffsetDateTime.now();
    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    private volatile AnalysisResult result;
    private volatile String errorMessage;
    private volatile String errorKind;

    public AnalysisJob(String id, String fileName) {
        this.id = id;
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Status getStatus() {
        return status.get();
    }

    public void setStatus(Status s) {
        status.set(s);
    }

    public void setStageFromOrchestrator(AnalysisOrchestrator.Stage stage) {
        status.set(switch (stage) {
            case PARSING -> Status.PARSING;
            case EXTRACTING -> Status.EXTRACTING;
            case ANALYZING_INDIVIDUAL -> Status.ANALYZING_INDIVIDUAL;
            case ANALYZING_SET -> Status.ANALYZING_SET;
            case VERIFYING -> Status.VERIFYING;
            case SCORING -> Status.SCORING;
            case DONE -> Status.DONE;
        });
    }

    public AnalysisResult getResult() {
        return result;
    }

    public void setResult(AnalysisResult result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorKind() {
        return errorKind;
    }

    public void fail(String kind, String message) {
        this.errorKind = kind;
        this.errorMessage = message;
        this.status.set(Status.FAILED);
    }
}
