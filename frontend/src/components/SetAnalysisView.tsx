import { useState } from "react";
import type { AnalysisResult, ContradictionRecord } from "../types";

function ContradictionCard({ c }: { c: ContradictionRecord }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <div className="flex flex-wrap items-center gap-2">
        <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-700">
          {c.status === "CONFIRMED_ISSUE" ? "Подтверждено" : c.status === "POTENTIAL_ISSUE" ? "Предположение" : "Нужна проверка"}
        </span>
        <span className="font-mono text-xs text-slate-500">{c.requirementIds.join(" ⟷ ")}</span>
        <span className="text-xs text-slate-400">уверенность {(c.confidence * 100).toFixed(0)}%</span>
      </div>
      <p className="mt-2 text-sm text-slate-700">{c.explanation}</p>
      <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
        {c.requirementIds.map((id) => (
          <div key={id} className="rounded-lg bg-slate-50 p-2 text-xs text-slate-600">
            <span className="font-semibold text-slate-800">{id}: </span>«{c.quotes[id] ?? "—"}»
          </div>
        ))}
      </div>
      {c.possibleConsequence && (
        <p className="mt-2 text-xs text-slate-500">
          <b>Возможные последствия:</b> {c.possibleConsequence}
        </p>
      )}
      {c.resolutionSuggestion && (
        <p className="mt-1 text-xs text-brand-700">
          <b>Как разрешить:</b> {c.resolutionSuggestion}
        </p>
      )}
    </div>
  );
}

export default function SetAnalysisView({ result }: { result: AnalysisResult }) {
  const [tab, setTab] = useState<"contradictions" | "duplicates" | "traceability">("contradictions");

  return (
    <div>
      <div className="mb-4 flex gap-2 border-b border-slate-200">
        {[
          ["contradictions", `Противоречия (${result.contradictions.length})`],
          ["duplicates", `Дубликаты (${result.duplicates.length})`],
          ["traceability", "Матрица прослеживаемости"],
        ].map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key as typeof tab)}
            className={`-mb-px border-b-2 px-3 py-2 text-sm font-medium ${
              tab === key ? "border-brand-600 text-brand-700" : "border-transparent text-slate-500 hover:text-slate-700"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === "contradictions" && (
        <div className="space-y-4">
          {result.contradictions.length === 0 && (
            <p className="text-sm text-slate-500">Противоречий между требованиями не обнаружено.</p>
          )}
          {result.contradictions.map((c) => (
            <ContradictionCard key={c.id} c={c} />
          ))}
        </div>
      )}

      {tab === "duplicates" && (
        <div className="space-y-3">
          {result.duplicates.length === 0 && <p className="text-sm text-slate-500">Дублирующихся требований не обнаружено.</p>}
          {result.duplicates.map((d) => (
            <div key={d.id} className="rounded-xl border border-slate-200 bg-white p-4">
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs text-slate-500">{d.requirementIds.join(" ≈ ")}</span>
                <span className="text-xs text-slate-400">уверенность {(d.confidence * 100).toFixed(0)}%</span>
              </div>
              <p className="mt-1 text-sm text-slate-700">{d.explanation}</p>
            </div>
          ))}
        </div>
      )}

      {tab === "traceability" && (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-2">ID</th>
                <th className="px-4 py-2">Исходный ID</th>
                <th className="px-4 py-2">Связанные требования</th>
                <th className="px-4 py-2">Замечания</th>
                <th className="px-4 py-2">Нарушенные критерии</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {result.traceabilityMatrix.map((row) => (
                <tr key={row.internalId}>
                  <td className="px-4 py-2 font-mono text-xs">{row.internalId}</td>
                  <td className="px-4 py-2 text-xs text-slate-500">{row.originalId ?? "—"}</td>
                  <td className="px-4 py-2 text-xs">{row.relatedRequirementIds.join(", ") || "—"}</td>
                  <td className="px-4 py-2 text-xs">{row.findingIds.join(", ") || "—"}</td>
                  <td className="px-4 py-2 text-xs">{row.violatedCriteria.join(", ") || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
