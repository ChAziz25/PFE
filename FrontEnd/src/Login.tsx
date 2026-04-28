import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

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
    <div className="flex flex-col items-center justify-center bg-(--color-bg-main) text-(--color-text-main) min-h-screen w-screen p-4">
      <div className="flex flex-col items-center gap-6 bg-(--color-bg-card) p-10 rounded-2xl shadow-lg w-full max-w-md">
        {/* Header */}
        <div className="flex flex-col items-center gap-1 w-full">
          <h1 className="text-xl font-semibold tracking-tight">welcome back</h1>
          <p className="text-sm text-(--color-text-muted)">
            sign in to continue
          </p>
        </div>

        {/* Form Fields */}
        <div className="flex flex-col gap-4 w-full">
          <input
            type="email"
            placeholder="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted) text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
          />

          <input
            type="password"
            placeholder="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted) text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
          />
        </div>

        {/* Error Message */}
        {error && (
          <p className="text-sm text-(--color-error) w-full">{error}</p>
        )}

        {/* Submit Button */}
        <button
          onClick={handleLogin}
          className="w-full bg-(--color-primary) hover:bg-(--color-primary-hover) text-(--color-button-text) px-4 py-2 rounded-lg transition active:scale-[0.98]"
        >
          sign in
        </button>

        {/* Divider */}
        <hr className="w-full border-(--color-border)" />

        {/* Footer Link */}
        <p className="text-sm text-(--color-text-muted)">
          don't have an account?{" "}
          <Link
            to="/signup"
            className="text-(--color-primary) hover:underline bg-transparent border-none p-0 cursor-pointer"
          >
            sign up
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Login;
