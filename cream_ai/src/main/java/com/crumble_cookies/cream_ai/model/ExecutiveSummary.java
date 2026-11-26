package com.crumble_cookies.cream_ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Краткое резюме для руководителя/заказчика, понятное без технического образования. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutiveSummary {
    private String overallState;
    private int totalRequirements;
    private long confirmedIssues;
    private long potentialIssues;
    private long needsHumanReview;
    private long criticalDefects;
    private int healthScore;
    private List<String> mainRisks;
    private Verdict readiness;
    private List<String> topFixesFirst;
}
