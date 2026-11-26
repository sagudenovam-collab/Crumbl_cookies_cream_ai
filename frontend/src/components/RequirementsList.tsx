import { useMemo, useState } from "react";
import type { Finding, Requirement, Severity, EvidenceStatus } from "../types";

interface Props {
  requirements: Requirement[];
  findings: Finding[];
}

const STATUS_LABEL: Record<EvidenceStatus, string> = {
  CONFIRMED_ISSUE: "Подтверждено",
  POTENTIAL_ISSUE: "Предположение",
  NEEDS_HUMAN_REVIEW: "Нужна проверка",
};

const STATUS_COLOR: Record<EvidenceStatus, string> = {
  CONFIRMED_ISSUE: "bg-red-100 text-red-700",
  POTENTIAL_ISSUE: "bg-amber-100 text-amber-700",
  NEEDS_HUMAN_REVIEW: "bg-slate-200 text-slate-700",
};

const SEVERITY_BADGE: Record<Severity, string> = {
  CRITICAL: "bg-red-600 text-white",
  HIGH: "bg-orange-500 text-white",
  MEDIUM: "bg-amber-400 text-slate-900",
  LOW: "bg-slate-200 text-slate-700",
};

export default function RequirementsList({ requirements, findings }: Props) {
  const [search, setSearch] = useState("");
  const [severityFilter, setSeverityFilter] = useState<Severity | "ALL">("ALL");
  const [statusFilter, setStatusFilter] = useState<EvidenceStatus | "ALL">("ALL");
  const [expanded, setExpanded] = useState<string | null>(null);

  const findingsByReq = useMemo(() => {
    const map = new Map<string, Finding[]>();
    for (const f of findings) {
      for (const id of f.relatedRequirementIds) {
        if (!map.has(id)) map.set(id, []);
        map.get(id)!.push(f);
      }
    }
    return map;
  }, [findings]);

  const filtered = requirements.filter((r) => {
    const ownFindings = findingsByReq.get(r.internalId) ?? [];
    if (search) {
      const q = search.toLowerCase();
      const matches =
        r.internalId.toLowerCase().includes(q) ||
        (r.originalId ?? "").toLowerCase().includes(q) ||
        r.originalText.toLowerCase().includes(q);
      if (!matches) return false;
    }
    if (severityFilter !== "ALL" && !ownFindings.some((f) => f.severity === severityFilter)) return false;
    if (statusFilter !== "ALL" && !ownFindings.some((f) => f.status === statusFilter)) return false;
    return true;
  });

  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-3">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Поиск по тексту или ID..."
          className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none"
        />
        <select
          value={severityFilter}
          onChange={(e) => setSeverityFilter(e.target.value as Severity | "ALL")}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="ALL">Любая серьёзность</option>
          <option value="CRITICAL">CRITICAL</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LOW">LOW</option>
        </select>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as EvidenceStatus | "ALL")}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="ALL">Любой статус</option>
          <option value="CONFIRMED_ISSUE">Подтверждено</option>
          <option value="POTENTIAL_ISSUE">Предположение</option>
          <option value="NEEDS_HUMAN_REVIEW">Нужна проверка</option>
        </select>
      </div>

      <div className="space-y-3">
        {filtered.map((r) => {
          const ownFindings = findingsByReq.get(r.internalId) ?? [];
          const isOpen = expanded === r.internalId;
          return (
            <div key={r.internalId} className="rounded-xl border border-slate-200 bg-white">
              <button
                className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left"
                onClick={() => setExpanded(isOpen ? null : r.internalId)}
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-semibold text-brand-700">{r.internalId}</span>
                    {r.originalId && <span className="text-xs text-slate-400">({r.originalId})</span>}
                    {r.needsHumanReview && (
                      <span className="rounded-full bg-slate-200 px-2 py-0.5 text-[10px] font-medium text-slate-700">
                        на проверку человеку
                      </span>
                    )}
                  </div>
                  <div className="mt-1 text-sm text-slate-700">{r.summary || r.originalText}</div>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  {ownFindings.length === 0 ? (
                    <span className="rounded-full bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700">
                      ✓ без замечаний
                    </span>
                  ) : (
                    ownFindings
                      .slice(0, 3)
                      .map((f, i) => (
                        <span key={i} className={`rounded-full px-2 py-1 text-[10px] font-semibold ${SEVERITY_BADGE[f.severity]}`}>
                          {f.severity}
                        </span>
                      ))
                  )}
                  <span className="text-slate-400">{isOpen ? "▲" : "▼"}</span>
                </div>
              </button>

              {isOpen && (
                <div className="border-t border-slate-100 px-5 py-4">
                  <blockquote className="rounded-lg bg-slate-50 p-3 text-sm italic text-slate-700">
                    «{r.originalText}»
                  </blockquote>
                  <div className="mt-2 text-xs text-slate-500">
                    Тип: {r.type} · Источник: {r.source} ·{" "}
                    {r.location.page != null ? `стр. ${r.location.page}` : r.location.line != null ? `строка ${r.location.line}` : "местоположение не определено"}
                  </div>

                  {ownFindings.length === 0 ? (
                    <p className="mt-4 text-sm text-emerald-700">Замечаний не найдено — требование соответствует проверенным критериям.</p>
                  ) : (
                    <div className="mt-4 space-y-3">
                      {ownFindings.map((f) => (
                        <div key={f.id} className="rounded-lg border border-slate-200 p-3">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${SEVERITY_BADGE[f.severity]}`}>
                              {f.severity}
                            </span>
                            <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${STATUS_COLOR[f.status]}`}>
                              {STATUS_LABEL[f.status]}
                            </span>
                            <span className="text-xs font-medium text-slate-600">{f.criterion}</span>
                            <span className="text-xs text-slate-400">{f.priority}</span>
                          </div>
                          <p className="mt-2 text-sm text-slate-700">{f.explanation}</p>
                          <p className="mt-1 text-xs text-slate-500">Цитата: «{f.quote}»</p>
                          <p className="mt-1 text-xs text-slate-600"><b>Рекомендация:</b> {f.recommendation}</p>
                          {f.improvedWording && (
                            <p className="mt-1 text-xs text-brand-700"><b>Пример формулировки:</b> {f.improvedWording}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
        {filtered.length === 0 && (
          <div className="rounded-xl border border-dashed border-slate-300 py-10 text-center text-sm text-slate-500">
            Ничего не найдено по заданным фильтрам.
          </div>
        )}
      </div>
    </div>
  );
}
