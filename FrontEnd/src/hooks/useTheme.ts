import { useState, useEffect } from "react";

export function useTheme() {
  const [theme, setTheme] = useState(
    () => localStorage.getItem("theme") || "dark",
  );

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("theme", theme);

    document.documentElement.style.setProperty(
      "--shadow-card",
      theme === "dark"
        ? "0 0 0 1px rgba(0,212,255,0.05), 0 24px 48px rgba(0,0,0,0.5)"
        : "0 4px 24px rgba(0,0,0,0.08)",
    );
    document.documentElement.style.setProperty(
      "--shadow-panel",
      theme === "dark"
        ? "0 0 24px var(--color-primary-glow)"
        : "0 4px 24px rgba(0,212,255,0.1)",
    );
  }, [theme]);

  const toggleTheme = () => setTheme((t) => (t === "dark" ? "light" : "dark"));

  return { theme, toggleTheme };
}

export default useTheme;
