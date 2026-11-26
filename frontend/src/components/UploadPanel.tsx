import { useCallback, useRef, useState } from "react";

interface Props {
  onFileSelected: (file: File) => void;
  error: string | null;
}

const ACCEPTED = [".pdf", ".md", ".markdown", ".docx", ".txt"];

export default function UploadPanel({ onFileSelected, error }: Props) {
  const [dragActive, setDragActive] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFiles = useCallback(
    (files: FileList | null) => {
      if (!files || files.length === 0) return;
      onFileSelected(files[0]);
    },
    [onFileSelected]
  );

  return (
    <div className="mx-auto max-w-3xl px-6 py-16 text-center">
      <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
        Requirements Intelligence Agent
      </h1>
      <p className="mx-auto mt-4 max-w-xl text-base text-slate-600">
        Интеллектуальный агент для автоматического анализа качества технических требований
        перед началом разработки. Загрузите документ — агент найдёт противоречия,
        неоднозначности, неполноту и дублирование, а также рассчитает Requirement Health Score.
      </p>

      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragActive(true);
        }}
        onDragLeave={() => setDragActive(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragActive(false);
          handleFiles(e.dataTransfer.files);
        }}
        onClick={() => inputRef.current?.click()}
        className={`mt-10 cursor-pointer rounded-2xl border-2 border-dashed px-8 py-14 transition-colors ${
          dragActive ? "border-brand-500 bg-brand-50" : "border-slate-300 bg-white hover:border-brand-400"
        }`}
      >
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED.join(",")}
          className="hidden"
          onChange={(e) => handleFiles(e.target.files)}
        />
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-brand-100 text-brand-600">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="h-7 w-7">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 16.5V9m0 0l-3 3m3-3l3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3.75 3.75 0 014.133 4.303A4.5 4.5 0 0117.25 19.5H6.75z" />
          </svg>
        </div>
        <p className="text-sm font-medium text-slate-900">Перетащите файл сюда или нажмите для выбора</p>
        <p className="mt-1 text-xs text-slate-500">Поддерживаются: PDF, Markdown (.md), DOCX, TXT — до 25 МБ</p>
      </div>

      {error && (
        <div className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="mt-10 grid grid-cols-1 gap-4 text-left sm:grid-cols-3">
        {[
          ["Evidence-First", "Каждое замечание подкреплено точной цитатой и местоположением в документе."],
          ["Health Score", "Прозрачная методика оценки качества набора требований от 0 до 100."],
          ["Contradiction Detective", "Находит противоречия и дубликаты между требованиями."],
        ].map(([title, desc]) => (
          <div key={title} className="rounded-xl border border-slate-200 bg-white p-4">
            <div className="text-sm font-semibold text-slate-900">{title}</div>
            <div className="mt-1 text-xs text-slate-500">{desc}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
