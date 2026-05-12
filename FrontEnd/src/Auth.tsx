import { useState } from "react";
import { Link } from "react-router-dom";

export default function Auth() {
  const [user] = useState(() => {
    const stored = localStorage.getItem("user");
    return stored ? JSON.parse(stored) : null;
  });

  const handleLogout = () => {
    localStorage.removeItem("user");
    window.location.href = "/login";
  };

  return (
    <div className="fixed top-5 right-5 z-50">
      {user ? (
        <div className="flex items-center gap-2 bg-(--color-bg-card) border border-(--color-border-strong) rounded-xl px-3 py-1.5 shadow-card">
          <Link to="/profile" className="flex items-center gap-2 group">
            <div className="w-6 h-6 rounded-md bg-(--color-primary-light) border border-(--color-border-accent) flex items-center justify-center">
              <span className="text-[10px] text-(--color-primary) font-mono font-semibold">
                {user.name?.charAt(0).toUpperCase()}
              </span>
            </div>
            <span className="font-mono text-xs text-(--color-text-muted) group-hover:text-(--color-text-main) transition-colors">
              {user.name}
            </span>
          </Link>

          <div className="w-px h-4 bg-(--color-border-strong)" />

          <button
            onClick={handleLogout}
            className="font-mono text-xs text-(--color-error) hover:bg-(--color-error-bg) px-2 py-1 rounded-md transition-colors"
          >
            logout
          </button>
        </div>
      ) : (
        <Link
          to="/login"
          className="font-mono flex items-center gap-1.5 bg-(--color-primary) text-(--color-button-text) hover:opacity-90 rounded-xl px-4 py-2 text-xs font-semibold tracking-wider transition-all active:scale-[0.98]"
        >
          sign in →
        </Link>
      )}
    </div>
  );
}
