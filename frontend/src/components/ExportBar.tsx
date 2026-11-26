import { reportDownloadUrl } from "../api/client";

export default function ExportBar({ analysisId }: { analysisId: string }) {
  return (
    <div className="flex gap-2">
      <a
        href={reportDownloadUrl(analysisId, "md")}
        className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
      >
        ⬇ Markdown-отчёт
      </a>
      <a
        href={reportDownloadUrl(analysisId, "json")}
        className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50"
      >
        ⬇ JSON
      </a>
    </div>
  );
}
