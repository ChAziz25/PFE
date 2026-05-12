import { useTheme } from "./hooks/useTheme";

export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();

  return (
    <button
      onClick={toggleTheme}
      className="font-mono bg-(--color-bg-muted) border border-(--color-border-strong) text-(--color-text-muted) hover:text-(--color-text-main) rounded-xl px-3 py-1.5 text-xs tracking-wider transition-all"
    >
      {theme === "dark" ? "◐ light" : "◑ dark"}
    </button>
  );
}
