import { useNavigate } from "react-router-dom";

export default function BackButton() {
  const navigate = useNavigate();

  return (
    <button
      onClick={() => navigate(-1)}
      className="font-mono flex items-center gap-1.5 bg-(--color-bg-card) border border-(--color-border-strong) text-(--color-text-muted) hover:text-(--color-text-main) rounded-xl px-3 py-1.5 text-xs tracking-wider transition-all"
    >
      ← back
    </button>
  );
}
