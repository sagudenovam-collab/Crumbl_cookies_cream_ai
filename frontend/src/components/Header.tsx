export default function Header({ onReset }: { onReset?: () => void }) {
  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <button onClick={onReset} className="flex items-center gap-3 text-left">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-sm font-bold text-white">
            RI
          </div>
          <div>
            <div className="text-sm font-semibold text-slate-900">Requirements Intelligence Agent</div>
            <div className="text-xs text-slate-500">Анализ качества требований на базе ИИ</div>
          </div>
        </button>
        <a
          href="https://github.com/sagudenovam-collab/crumbl_cookies_cream_ai"
          target="_blank"
          rel="noreferrer"
          className="text-xs font-medium text-slate-500 hover:text-brand-600"
        >
          BCC-HUB × MNU FinTech Hackathon
        </a>
      </div>
    </header>
  );
}
