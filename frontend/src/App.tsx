import { useCallback, useEffect, useRef, useState } from "react";
import Header from "./components/Header";
import UploadPanel from "./components/UploadPanel";
import ProgressView from "./components/ProgressView";
import Dashboard from "./components/Dashboard";
import RequirementsList from "./components/RequirementsList";
import SetAnalysisView from "./components/SetAnalysisView";
import ExportBar from "./components/ExportBar";
import { ApiError, fetchFindings, fetchResult, fetchStatus, uploadDocument } from "./api/client";
import type { FindingsResponse } from "./api/client";
import type { AnalysisResult, JobStatus } from "./types";

type Phase = "upload" | "progress" | "results" | "error";

export default function App() {
  const [phase, setPhase] = useState<Phase>("upload");
  const [analysisId, setAnalysisId] = useState<string | null>(null);
  const [fileName, setFileName] = useState("");
  const [status, setStatus] = useState<JobStatus>("PENDING");
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [findings, setFindings] = useState<FindingsResponse | null>(null);
  const [tab, setTab] = useState<"dashboard" | "requirements" | "set">("dashboard");
  const pollRef = useRef<number | null>(null);

  const reset = useCallback(() => {
    if (pollRef.current) window.clearInterval(pollRef.current);
    setPhase("upload");
    setAnalysisId(null);
    setResult(null);
    setFindings(null);
    setError(null);
  }, []);

  const handleFile = useCallback(async (file: File) => {
    setError(null);
    setFileName(file.name);
    try {
      const id = await uploadDocument(file);
      setAnalysisId(id);
      setPhase("progress");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Не удалось загрузить файл.");
    }
  }, []);

  useEffect(() => {
    if (phase !== "progress" || !analysisId) return;

    const poll = async () => {
      try {
        const s = await fetchStatus(analysisId);
        setStatus(s.status);
        if (s.status === "DONE") {
          if (pollRef.current) window.clearInterval(pollRef.current);
          const [res, find] = await Promise.all([fetchResult(analysisId), fetchFindings(analysisId)]);
          setResult(res);
          setFindings(find);
          setPhase("results");
        } else if (s.status === "FAILED") {
          if (pollRef.current) window.clearInterval(pollRef.current);
          setError(s.errorMessage ?? "Анализ завершился с ошибкой.");
          setPhase("error");
        }
      } catch (e) {
        if (pollRef.current) window.clearInterval(pollRef.current);
        setError(e instanceof ApiError ? e.message : "Не удалось получить статус анализа.");
        setPhase("error");
      }
    };

    poll();
    pollRef.current = window.setInterval(poll, 1500);
    return () => {
      if (pollRef.current) window.clearInterval(pollRef.current);
    };
  }, [phase, analysisId]);

  const allFindings = findings ? [...findings.individualFindings, ...findings.setLevelFindings] : [];

  return (
    <div className="min-h-screen bg-slate-50">
      <Header onReset={reset} />

      {phase === "upload" && <UploadPanel onFileSelected={handleFile} error={error} />}

      {phase === "progress" && <ProgressView status={status} fileName={fileName} />}

      {phase === "error" && (
        <div className="mx-auto max-w-xl px-6 py-20 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100 text-2xl text-red-600">
            !
          </div>
          <h2 className="text-lg font-semibold text-slate-900">Не удалось завершить анализ</h2>
          <p className="mt-2 text-sm text-slate-600">{error}</p>
          <button
            onClick={reset}
            className="mt-6 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
          >
            Попробовать снова
          </button>
        </div>
      )}

      {phase === "results" && result && analysisId && (
        <div className="mx-auto max-w-6xl px-6 py-8">
          <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h1 className="text-xl font-semibold text-slate-900">{result.passport.documentName}</h1>
              <p className="text-sm text-slate-500">{result.verdictExplanation}</p>
            </div>
            <ExportBar analysisId={analysisId} />
          </div>

          <div className="mb-6 flex gap-2 border-b border-slate-200">
            {[
              ["dashboard", "Дашборд"],
              ["requirements", "Требования"],
              ["set", "Анализ набора"],
            ].map(([key, label]) => (
              <button
                key={key}
                onClick={() => setTab(key as typeof tab)}
                className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
                  tab === key ? "border-brand-600 text-brand-700" : "border-transparent text-slate-500 hover:text-slate-700"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {tab === "dashboard" && <Dashboard result={result} />}
          {tab === "requirements" && (
            <RequirementsList requirements={result.requirements} findings={allFindings} />
          )}
          {tab === "set" && <SetAnalysisView result={result} />}
        </div>
      )}
    </div>
  );
}
