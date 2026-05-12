import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import ThemeToggle from "./ThemeToggle";
import BackButton from "./BackButton";

function Login() {
  const [theme, setTheme] = useState("dark");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const handleLogin = () => {
    fetch("http://localhost:8080/api/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    })
      .then((res) => {
        if (!res.ok)
          return res.json().then((data) => {
            setError(data.error);
            throw new Error(data.error);
          });
        return res.json();
      })
      .then((data) => {
        localStorage.setItem("user", JSON.stringify(data));
        window.location.href = "/";
      })
      .catch((error) => {
        setError(error.message);
      });
  };

  return (
    <div className="relative flex flex-col items-center justify-center min-h-screen w-screen p-6 overflow-hidden bg-(--color-bg-main) text-(--color-text-main)">
      {/* Grid texture */}
      <div className="absolute inset-0 pointer-events-none opacity-60 grid-texture" />

      {/* Cyan orb */}
      <div className="absolute pointer-events-none rounded-full cyan-orb" />

      {/* Back button */}
      <div className="relative z-10 flex justify-between w-full max-w-sm mb-4">
        <BackButton />
        <ThemeToggle />
      </div>

      {/* Main card */}
      <div className="relative z-10 flex flex-col gap-5 w-full max-w-sm bg-(--color-bg-card) border border-(--color-border-strong) rounded-2xl p-7 shadow-card">
        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-(--color-primary-light) border border-(--color-border-accent) flex items-center justify-center">
            <span className="text-sm text-(--color-primary)">⬡</span>
          </div>
          <div>
            <p className="text-sm font-semibold m-0 tracking-tight">
              welcome back
            </p>
            <p className="font-mono text-[11px] text-(--color-text-muted) m-0">
              sign in to continue
            </p>
          </div>
        </div>

        {/* Divider */}
        <div className="h-px bg-(--color-border) -mx-7" />

        {/* Fields */}
        <div className="flex flex-col gap-3">
          <p className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest m-0">
            Credentials
          </p>

          {[
            {
              type: "email",
              placeholder: "email address",
              value: email,
              setter: setEmail,
              icon: "◎",
            },
            {
              type: "password",
              placeholder: "password",
              value: password,
              setter: setPassword,
              icon: "◈",
            },
          ].map(({ type, placeholder, value, setter, icon }) => (
            <div key={type} className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[13px] text-(--color-primary) pointer-events-none">
                {icon}
              </span>
              <input
                type={type}
                placeholder={placeholder}
                value={value}
                onChange={(e) => setter(e.target.value)}
                className="font-mono w-full pl-9 pr-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) focus:border-(--color-border-accent) transition"
              />
            </div>
          ))}
        </div>

        {/* Error */}
        {error && (
          <p className="font-mono text-[12px] text-(--color-error) bg-(--color-error-bg) border border-(--color-error-border) px-3 py-2 rounded-lg m-0">
            ⚠ {error}
          </p>
        )}

        {/* Submit */}
        <button
          onClick={handleLogin}
          className="font-mono w-full py-2.5 bg-(--color-primary) text-(--color-button-text) rounded-lg text-[13px] font-semibold tracking-wider transition active:scale-[0.985] hover:opacity-90"
        >
          sign in →
        </button>

        {/* Divider */}
        <div className="h-px bg-(--color-border) -mx-7" />

        {/* Footer */}
        <p className="font-mono text-[12px] text-(--color-text-muted) text-center m-0">
          don't have an account?{" "}
          <Link
            to="/signup"
            className="text-(--color-primary) hover:opacity-75 transition-opacity"
          >
            sign up →
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Login;
