package com.crumble_cookies.cream_ai.analysis;

import com.crumble_cookies.cream_ai.extraction.RequirementExtractor;
import com.crumble_cookies.cream_ai.extraction.RequirementNormalizer;
import com.crumble_cookies.cream_ai.llm.LLMException;
import com.crumble_cookies.cream_ai.llm.LLMProvider;
import com.crumble_cookies.cream_ai.model.AnalysisPassport;
import com.crumble_cookies.cream_ai.model.AnalysisResult;
import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.DuplicateRecord;
import com.crumble_cookies.cream_ai.model.EvidenceStatus;
import com.crumble_cookies.cream_ai.model.ExecutiveSummary;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.HealthScoreBreakdown;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.model.Severity;
import com.crumble_cookies.cream_ai.model.TraceabilityRow;
import com.crumble_cookies.cream_ai.parser.DocumentParserRegistry;
import com.crumble_cookies.cream_ai.parser.DocumentParsingException;
import com.crumble_cookies.cream_ai.parser.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Главный оркестратор: связывает все этапы конвейера анализа от разбора
 * документа до итогового AnalysisResult. Ни на одном этапе не подставляет
 * фиктивный результат вместо реального анализа — любая ошибка LLM
 * пробрасывается наверх как AnalysisException.
 */
@Component
public class AnalysisOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrator.class);
    public static final String AGENT_VERSION = "1.0.0";

    private final DocumentParserRegistry parserRegistry;
    private final RequirementExtractor requirementExtractor;
    private final RequirementNormalizer requirementNormalizer;
    private final IndividualRequirementAnalyzer individualRequirementAnalyzer;
    private final RequirementSetAnalyzer requirementSetAnalyzer;
    private final ContradictionDetector contradictionDetector;
    private final EvidenceVerifier evidenceVerifier;
    private final SeverityClassifier severityClassifier;
    private final RequirementScorer requirementScorer;
    private final TraceabilityAnalyzer traceabilityAnalyzer;
    private final VerdictCalculator verdictCalculator;
    private final LLMProvider llmProvider;

    public AnalysisOrchestrator(DocumentParserRegistry parserRegistry,
                                 RequirementExtractor requirementExtractor,
                                 RequirementNormalizer requirementNormalizer,
                                 IndividualRequirementAnalyzer individualRequirementAnalyzer,
                                 RequirementSetAnalyzer requirementSetAnalyzer,
                                 ContradictionDetector contradictionDetector,
                                 EvidenceVerifier evidenceVerifier,
                                 SeverityClassifier severityClassifier,
                                 RequirementScorer requirementScorer,
                                 TraceabilityAnalyzer traceabilityAnalyzer,
                                 VerdictCalculator verdictCalculator,
                                 LLMProvider llmProvider) {
        this.parserRegistry = parserRegistry;
        this.requirementExtractor = requirementExtractor;
        this.requirementNormalizer = requirementNormalizer;
        this.individualRequirementAnalyzer = individualRequirementAnalyzer;
        this.requirementSetAnalyzer = requirementSetAnalyzer;
        this.contradictionDetector = contradictionDetector;
        this.evidenceVerifier = evidenceVerifier;
        this.severityClassifier = severityClassifier;
        this.requirementScorer = requirementScorer;
        this.traceabilityAnalyzer = traceabilityAnalyzer;
        this.verdictCalculator = verdictCalculator;
        this.llmProvider = llmProvider;
    }

    public enum Stage {
        PARSING, EXTRACTING, ANALYZING_INDIVIDUAL, ANALYZING_SET, VERIFYING, SCORING, DONE
    }

    public AnalysisResult run(Path filePath, String fileName) throws DocumentParsingException, LLMException {
        return run(filePath, fileName, stage -> {
        });
    }

    public AnalysisResult run(Path filePath, String fileName, Consumer<Stage> progress)
            throws DocumentParsingException, LLMException {

        progress.accept(Stage.PARSING);
        ParsedDocument document = parserRegistry.parse(filePath, fileName);
        if (document.isEmpty()) {
            log.warn("Документ пуст или не содержит текста: {}", fileName);
        }

        progress.accept(Stage.EXTRACTING);
        List<Requirement> rawRequirements = document.isEmpty()
                ? List.of()
                : requirementExtractor.extract(document);
        List<Requirement> requirements = requirementNormalizer.normalize(rawRequirements);
        log.info("Извлечено требований: {}", requirements.size());

        Map<String, Requirement> byId = new LinkedHashMap<>();
        for (Requirement r : requirements) {
            byId.put(r.getInternalId(), r);
        }

        progress.accept(Stage.ANALYZING_INDIVIDUAL);
        List<Finding> individualFindings = requirements.isEmpty()
                ? List.of()
                : individualRequirementAnalyzer.analyze(requirements);

        progress.accept(Stage.ANALYZING_SET);
        RequirementSetAnalyzer.SetAnalysisOutput setOutput = requirements.size() < 2
                ? new RequirementSetAnalyzer.SetAnalysisOutput(List.of(), List.of(), List.of())
                : requirementSetAnalyzer.analyze(requirements);
        List<ContradictionRecord> contradictions = contradictionDetector.refine(new ArrayList<>(setOutput.contradictions()));
        List<DuplicateRecord> duplicates = new ArrayList<>(setOutput.duplicates());
        List<Finding> setFindings = new ArrayList<>(setOutput.otherFindings());

        progress.accept(Stage.VERIFYING);
        evidenceVerifier.verifyIndividualFindings(individualFindings, byId);
        evidenceVerifier.verifySetFindings(setFindings, byId);
        evidenceVerifier.verifyContradictions(contradictions, byId);
        evidenceVerifier.verifyDuplicates(duplicates);
        severityClassifier.classify(individualFindings);
        severityClassifier.classify(setFindings);

        progress.accept(Stage.SCORING);
        HealthScoreBreakdown healthScore = requirementScorer.score(
                requirements, individualFindings, setFindings, contradictions, duplicates);

        List<TraceabilityRow> traceability = traceabilityAnalyzer.build(
                requirements, individualFindings, setFindings, contradictions, duplicates);

        List<Finding> allFindings = new ArrayList<>(individualFindings);
        allFindings.addAll(setFindings);

        VerdictCalculator.Result verdictResult = verdictCalculator.calculate(
                requirements, allFindings, contradictions, duplicates, healthScore.getOverallScore());

        ExecutiveSummary executiveSummary = buildExecutiveSummary(
                requirements, allFindings, contradictions, healthScore, verdictResult);

        List<String> needsHumanReview = collectNeedsHumanReview(requirements, allFindings, contradictions, duplicates);

        AnalysisPassport passport = AnalysisPassport.builder()
                .documentName(fileName)
                .analyzedAt(OffsetDateTime.now())
                .modelUsed(llmProvider.modelName())
                .agentVersion(AGENT_VERSION)
                .gitCommit(System.getenv().getOrDefault("GIT_COMMIT", readGitCommitSafely()))
                .pagesProcessed(document.totalPages())
                .requirementsExtracted(requirements.size())
                .build();

        progress.accept(Stage.DONE);

        return AnalysisResult.builder()
                .passport(passport)
                .executiveSummary(executiveSummary)
                .healthScore(healthScore)
                .requirements(requirements)
                .individualFindings(individualFindings)
                .setLevelFindings(setFindings)
                .contradictions(contradictions)
                .duplicates(duplicates)
                .traceabilityMatrix(traceability)
                .verdict(verdictResult.verdict())
                .verdictExplanation(verdictResult.explanation())
                .itemsNeedingHumanReview(needsHumanReview)
                .build();
    }

    private ExecutiveSummary buildExecutiveSummary(List<Requirement> requirements, List<Finding> allFindings,
                                                     List<ContradictionRecord> contradictions,
                                                     HealthScoreBreakdown healthScore,
                                                     VerdictCalculator.Result verdictResult) {
        long confirmed = allFindings.stream().filter(f -> f.getStatus() == EvidenceStatus.CONFIRMED_ISSUE).count()
                + contradictions.stream().filter(c -> c.getStatus() == EvidenceStatus.CONFIRMED_ISSUE).count();
        long potential = allFindings.stream().filter(f -> f.getStatus() == EvidenceStatus.POTENTIAL_ISSUE).count()
                + contradictions.stream().filter(c -> c.getStatus() == EvidenceStatus.POTENTIAL_ISSUE).count();
        long review = allFindings.stream().filter(f -> f.getStatus() == EvidenceStatus.NEEDS_HUMAN_REVIEW).count()
                + contradictions.stream().filter(c -> c.getStatus() == EvidenceStatus.NEEDS_HUMAN_REVIEW).count();
        long critical = allFindings.stream()
                .filter(f -> f.getStatus() == EvidenceStatus.CONFIRMED_ISSUE && f.getSeverity() == Severity.CRITICAL)
                .count();

        List<String> risks = new ArrayList<>();
        allFindings.stream()
                .filter(f -> f.getStatus() == EvidenceStatus.CONFIRMED_ISSUE
                        && (f.getSeverity() == Severity.CRITICAL || f.getSeverity() == Severity.HIGH))
                .limit(5)
                .forEach(f -> risks.add("[" + f.getSeverity() + "] "
                        + String.join(", ", f.getRelatedRequirementIds()) + ": " + f.getExplanation()));
        contradictions.stream()
                .filter(c -> c.getStatus() == EvidenceStatus.CONFIRMED_ISSUE)
                .limit(5)
                .forEach(c -> risks.add("[Противоречие] " + String.join(", ", c.getRequirementIds())
                        + ": " + c.getExplanation()));
        if (risks.isEmpty()) {
            risks.add("Подтверждённых критичных или серьёзных рисков не обнаружено.");
        }

        List<String> topFixes = new ArrayList<>();
        allFindings.stream()
                .filter(f -> f.getStatus() == EvidenceStatus.CONFIRMED_ISSUE && f.getSeverity() == Severity.CRITICAL)
                .limit(5)
                .forEach(f -> topFixes.add(String.join(", ", f.getRelatedRequirementIds()) + ": " + f.getRecommendation()));
        if (topFixes.isEmpty()) {
            topFixes.add("Срочных исправлений не требуется.");
        }

        return ExecutiveSummary.builder()
                .overallState(describeState(verdictResult))
                .totalRequirements(requirements.size())
                .confirmedIssues(confirmed)
                .potentialIssues(potential)
                .needsHumanReview(review)
                .criticalDefects(critical)
                .healthScore(healthScore.getOverallScore())
                .mainRisks(risks)
                .readiness(verdictResult.verdict())
                .topFixesFirst(topFixes)
                .build();
    }

    private String describeState(VerdictCalculator.Result verdictResult) {
        return switch (verdictResult.verdict()) {
            case READY -> "Документ готов к следующему этапу разработки.";
            case READY_WITH_RESERVATIONS -> "Документ в целом пригоден, но есть замечания, которые стоит учесть.";
            case NOT_READY -> "В документе есть дефекты, требующие устранения до начала разработки.";
            case INSUFFICIENT_INFORMATION -> "Недостаточно информации для надёжного вывода о готовности документа.";
        };
    }

    private List<String> collectNeedsHumanReview(List<Requirement> requirements, List<Finding> allFindings,
                                                    List<ContradictionRecord> contradictions,
                                                    List<DuplicateRecord> duplicates) {
        List<String> items = new ArrayList<>();
        requirements.stream().filter(Requirement::isNeedsHumanReview)
                .forEach(r -> items.add(r.getInternalId() + ": " + r.getHumanReviewReason()));
        allFindings.stream().filter(f -> f.getStatus() == EvidenceStatus.NEEDS_HUMAN_REVIEW)
                .forEach(f -> items.add(f.getId() + ": " + f.getVerificationNote()));
        contradictions.stream().filter(c -> c.getStatus() == EvidenceStatus.NEEDS_HUMAN_REVIEW)
                .forEach(c -> items.add(c.getId() + ": требуется проверка противоречия между "
                        + String.join(", ", c.getRequirementIds())));
        duplicates.stream().filter(d -> d.getStatus() == EvidenceStatus.NEEDS_HUMAN_REVIEW)
                .forEach(d -> items.add(d.getId() + ": требуется проверка дублирования между "
                        + String.join(", ", d.getRequirementIds())));
        return items;
    }

    private String readGitCommitSafely() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            String out = new String(process.getInputStream().readAllBytes()).trim();
            return out.isBlank() ? null : out;
        } catch (Exception e) {
            return null;
        }
    }
}
