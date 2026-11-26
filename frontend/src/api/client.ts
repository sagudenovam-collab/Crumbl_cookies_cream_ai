import type { AnalysisResult, ContradictionRecord, DuplicateRecord, Finding, Requirement, StatusResponse } from "../types";

const API_BASE = "/api/analyses";

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function handle<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let message = `Ошибка запроса (HTTP ${response.status})`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
    } catch {
      // ignore
    }
    throw new ApiError(response.status, message);
  }
  return response.json() as Promise<T>;
}

export async function uploadDocument(file: File): Promise<string> {
  const formData = new FormData();
  formData.append("file", file);
  const response = await fetch(API_BASE, { method: "POST", body: formData });
  const data = await handle<{ analysisId: string }>(response);
  return data.analysisId;
}

export async function fetchStatus(id: string): Promise<StatusResponse> {
  const response = await fetch(`${API_BASE}/${id}/status`);
  return handle<StatusResponse>(response);
}

export async function fetchResult(id: string): Promise<AnalysisResult> {
  const response = await fetch(`${API_BASE}/${id}/result`);
  return handle<AnalysisResult>(response);
}

export async function fetchRequirements(id: string): Promise<Requirement[]> {
  const response = await fetch(`${API_BASE}/${id}/requirements`);
  return handle<Requirement[]>(response);
}

export interface FindingsResponse {
  individualFindings: Finding[];
  setLevelFindings: Finding[];
  contradictions: ContradictionRecord[];
  duplicates: DuplicateRecord[];
}

export async function fetchFindings(id: string): Promise<FindingsResponse> {
  const response = await fetch(`${API_BASE}/${id}/findings`);
  return handle<FindingsResponse>(response);
}

export function reportDownloadUrl(id: string, format: "md" | "json"): string {
  return format === "json" ? `${API_BASE}/${id}/report?format=json` : `${API_BASE}/${id}/report`;
}
