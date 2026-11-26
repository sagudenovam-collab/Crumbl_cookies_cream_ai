import type { AnalysisResult, Severity, Verdict } from "../types";

const VERDICT_STYLE: Record<Verdict, { label: string; color: string }> = {
  READY: { label: "READY — готов к следующему этапу", color: "bg-emerald-100 text-emerald-800 border-emerald-200" },
  READY_WITH_RESERVATIONS: {
    label: "READY_WITH_RESERVATIONS — можно продолжать с оговорками",
    color: "bg-amber-100 text-amber-800 border-amber-200",
  },
  NOT_READY: { label: "NOT_READY — требуются исправления", color: "bg-red-100 text-red-800 border-red-200" },
  INSUFFICIENT_INFORMATION: {
    label: "INSUFFICIENT_INFORMATION — недостаточно данных",
    color: "bg-slate-100 text-slate-700 border-slate-200",
  },
};

const SEVERITY_COLOR: Record<Severity, string> = {
  CRITICAL: "bg-red-500",
  HIGH: "bg-orange-500",
  MEDIUM: "bg-amber-400",
  LOW: "bg-slate-300",
};

function ScoreRing({ score }: { score: number }) {
  const color = score >= 80 ? "text-emerald-500" : score >= 50 ? "text-amber-500" : "text-red-500";
  const circumference = 2 * Math.PI * 54;
  const offset = circumference * (1 - score / 100);
  return (
    <div className="relative flex h-36 w-36 items-center justify-center">
      <svg className="h-36 w-36 -rotate-90" viewBox="0 0 120 120">
        <circle cx="60" cy="60" r="54" fill="none" stroke="#e2e8f0" strokeWidth="10" />
        <circle
          cx="60"
          cy="60"
          r="54"
          fill="none"
          strokeWidth="10"
          strokeLinecap="round"
          className={color}
          stroke="currentColor"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className="text-3xl font-bold text-slate-900">{score}</span>
        <span className="text-xs text-slate-500">из 100</span>
      </div>
    </div>
  );
}

export default function Dashboard({ result }: { result: AnalysisResult }) {
  const { executiveSummary: s, healthScore, passport } = result;
  const verdict = VERDICT_STYLE[result.verdict];
  const maxCount = Math.max(1, ...Object.values(healthScore.severityDistribution));

  return (
    <div className="space-y-6">
      <div className={`rounded-xl border px-5 py-4 text-sm font-medium ${verdict.color}`}>{verdict.label}</div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
        <div className="flex flex-col items-center justify-center rounded-xl border border-slate-200 bg-white p-6">
          <ScoreRing score={healthScore.overallScore} />
          <div className="mt-3 text-xs text-slate-500 text-center">
            Requirement Health Score — собственная методика продукта, не официальная оценка ISO
          </div>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-6 md:col-span-2">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Stat label="Всего требований" value={s.totalRequirements} />
            <Stat label="Подтверждённых дефектов" value={s.confirmedIssues} accent="text-red-600" />
            <Stat label="Потенциальных проблем" value={s.potentialIssues} accent="text-amber-600" />
            <Stat label="Критичных дефектов" value={s.criticalDefects} accent="text-red-700" />
          </div>
          <div className="mt-5">
            <div className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-500">
              Распределение по серьёзности
            </div>
            <div className="space-y-1.5">
              {(Object.keys(SEVERITY_COLOR) as Severity[]).map((sev) => {
                const count = healthScore.severityDistribution[sev] ?? 0;
                return (
                  <div key={sev} className="flex items-center gap-2 text-xs">
                    <span className="w-16 text-slate-500">{sev}</span>
                    <div className="h-2 flex-1 rounded-full bg-slate-100">
                      <div
                        className={`h-2 rounded-full ${SEVERITY_COLOR[sev]}`}
                        style={{ width: `${(count / maxCount) * 100}%` }}
                      />
                    </div>
                    <span className="w-6 text-right text-slate-700">{count}</span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <h3 className="text-sm font-semibold text-slate-900">Главные риски</h3>
          <ul className="mt-3 space-y-2 text-sm text-slate-600">
            {s.mainRisks.map((risk, i) => (
              <li key={i} className="flex gap-2">
                <span className="text-red-500">●</span>
                <span>{risk}</span>
              </li>
            ))}
          </ul>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <h3 className="text-sm font-semibold text-slate-900">Что исправить в первую очередь</h3>
          <ul className="mt-3 space-y-2 text-sm text-slate-600">
            {s.topFixesFirst.map((fix, i) => (
              <li key={i} className="flex gap-2">
                <span className="text-brand-500">→</span>
                <span>{fix}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h3 className="text-sm font-semibold text-slate-900">Паспорт анализа</h3>
        <dl className="mt-3 grid grid-cols-2 gap-3 text-xs sm:grid-cols-4">
          <Field label="Документ" value={passport.documentName} />
          <Field label="Модель" value={passport.modelUsed} />
          <Field label="Версия агента" value={passport.agentVersion} />
          <Field label="Дата анализа" value={new Date(passport.analyzedAt).toLocaleString("ru-RU")} />
        </dl>
      </div>
    </div>
  );
}

function Stat({ label, value, accent }: { label: string; value: number; accent?: string }) {
  return (
    <div>
      <div className={`text-2xl font-bold ${accent ?? "text-slate-900"}`}>{value}</div>
      <div className="text-xs text-slate-500">{label}</div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-slate-400">{label}</dt>
      <dd className="font-medium text-slate-800">{value}</dd>
    </div>
  );
}
