import type { JobStatus } from "../types";

const STAGES: { key: JobStatus; label: string }[] = [
  { key: "PARSING", label: "Загрузка и разбор документа" },
  { key: "EXTRACTING", label: "Извлечение требований" },
  { key: "ANALYZING_INDIVIDUAL", label: "Анализ отдельных требований" },
  { key: "ANALYZING_SET", label: "Поиск противоречий и дубликатов" },
  { key: "VERIFYING", label: "Проверка доказательств" },
  { key: "SCORING", label: "Расчёт Health Score и отчёта" },
];

export default function ProgressView({ status, fileName }: { status: JobStatus; fileName: string }) {
  const currentIndex = STAGES.findIndex((s) => s.key === status);

  return (
    <div className="mx-auto max-w-xl px-6 py-20">
      <h2 className="text-center text-lg font-semibold text-slate-900">Анализируем документ «{fileName}»</h2>
      <p className="mt-1 text-center text-sm text-slate-500">
        Это реальный прогресс выполнения на бэкенде — не анимация.
      </p>
      <ol className="mt-10 space-y-4">
        {STAGES.map((stage, idx) => {
          const done = currentIndex > idx || status === "DONE";
          const active = currentIndex === idx;
          return (
            <li key={stage.key} className="flex items-center gap-3">
              <span
                className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                  done
                    ? "bg-emerald-500 text-white"
                    : active
                      ? "bg-brand-600 text-white animate-pulse"
                      : "bg-slate-200 text-slate-400"
                }`}
              >
                {done ? "✓" : idx + 1}
              </span>
              <span className={`text-sm ${active ? "font-semibold text-slate-900" : "text-slate-600"}`}>
                {stage.label}
              </span>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
