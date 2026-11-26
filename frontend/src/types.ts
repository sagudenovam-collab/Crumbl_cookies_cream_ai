export type Severity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
export type Priority = "P0" | "P1" | "P2" | "P3";
export type EvidenceStatus = "CONFIRMED_ISSUE" | "POTENTIAL_ISSUE" | "NEEDS_HUMAN_REVIEW";
export type Verdict = "READY" | "READY_WITH_RESERVATIONS" | "NOT_READY" | "INSUFFICIENT_INFORMATION";
export type RequirementType =
  | "FUNCTIONAL"
  | "NON_FUNCTIONAL"
  | "BUSINESS"
  | "INTERFACE"
  | "CONSTRAINT"
  | "UNKNOWN";

export type JobStatus =
  | "PENDING"
  | "PARSING"
  | "EXTRACTING"
  | "ANALYZING_INDIVIDUAL"
  | "ANALYZING_SET"
  | "VERIFYING"
  | "SCORING"
  | "DONE"
  | "FAILED";

export interface SourceLocation {
  sourceFile: string;
  page: number | null;
  line: number | null;
}

export interface Requirement {
  internalId: string;
  originalId: string | null;
  originalText: string;
  summary: string;
  source: string;
  location: SourceLocation;
  type: RequirementType;
  needsHumanReview: boolean;
  humanReviewReason: string | null;
}

export interface Finding {
  id: string;
  relatedRequirementIds: string[];
  criterion: string;
  quote: string;
  location: SourceLocation | null;
  explanation: string;
  severity: Severity;
  severityReason: string;
  priority: Priority;
  confidence: number;
  recommendation: string;
  improvedWording: string | null;
  status: EvidenceStatus;
  verificationNote: string | null;
  scope: "INDIVIDUAL" | "SET";
}

export interface ContradictionRecord {
  id: string;
  requirementIds: string[];
  quotes: Record<string, string>;
  explanation: string;
  possibleConsequence: string;
  resolutionSuggestion: string;
  status: EvidenceStatus;
  confidence: number;
}

export interface DuplicateRecord {
  id: string;
  requirementIds: string[];
  explanation: string;
  status: EvidenceStatus;
  confidence: number;
}

export interface TraceabilityRow {
  internalId: string;
  originalId: string | null;
  relatedRequirementIds: string[];
  findingIds: string[];
  violatedCriteria: string[];
  recommendations: string[];
}

export interface HealthScoreBreakdown {
  overallScore: number;
  byCriterion: Record<string, number>;
  severityDistribution: Record<Severity, number>;
  setLevelPenalty: number;
  evidenceConfidenceFactor: number;
  explanationNotes: string[];
}

export interface ExecutiveSummary {
  overallState: string;
  totalRequirements: number;
  confirmedIssues: number;
  potentialIssues: number;
  needsHumanReview: number;
  criticalDefects: number;
  healthScore: number;
  mainRisks: string[];
  readiness: Verdict;
  topFixesFirst: string[];
}

export interface AnalysisPassport {
  documentName: string;
  analyzedAt: string;
  modelUsed: string;
  agentVersion: string;
  gitCommit: string | null;
  pagesProcessed: number | null;
  requirementsExtracted: number;
}

export interface AnalysisResult {
  passport: AnalysisPassport;
  executiveSummary: ExecutiveSummary;
  healthScore: HealthScoreBreakdown;
  requirements: Requirement[];
  individualFindings: Finding[];
  setLevelFindings: Finding[];
  contradictions: ContradictionRecord[];
  duplicates: DuplicateRecord[];
  traceabilityMatrix: TraceabilityRow[];
  verdict: Verdict;
  verdictExplanation: string;
  itemsNeedingHumanReview: string[];
}

export interface StatusResponse {
  analysisId: string;
  fileName: string;
  status: JobStatus;
  createdAt: string;
  errorKind: string | null;
  errorMessage: string | null;
}
