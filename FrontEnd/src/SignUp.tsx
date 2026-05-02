import { useEffect, useState } from "react";

function SignUp() {
  const [theme, setTheme] = useState("dark");

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [error, setError] = useState("");

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const handleSignUp = () => {
    fetch("http://localhost:8080/api/register", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name, email, password }),
    })
      .then((res) => {
        res.json().then((data) => {
          console.log(data);
          window.location.href = "/login";
        });
      })
      .catch((error) => {
        console.error("Error:", error);
        setError(error.message);
      });
  };
  return (
    <div className="flex flex-col items-center justify-center bg-(--color-bg-main) text-(--color-text-main) min-h-screen w-screen p-4">
      <div className="flex flex-col items-center gap-6 bg-(--color-bg-card) p-10 rounded-2xl shadow-lg w-full max-w-md">
        {/* Header */}
        <div className="flex flex-col items-center gap-1 w-full">
          <h1 className="text-xl font-semibold tracking-tight">
            create account
          </h1>
          <p className="text-sm text-(--color-text-muted)">get started</p>
        </div>

        {/* Form Fields */}
        <div className="flex flex-col gap-4 w-full">
          <input
            type="text"
            placeholder="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted) text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
          />

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

          <input
            type="password"
            placeholder="confirm password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted) text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
          />
        </div>

        {/* Error Message */}
        {error && (
          <p className="text-sm text-(--color-error) w-full">{error}</p>
        )}

        {/* Submit Button */}
        <button
          onClick={handleSignUp}
          className="w-full bg-(--color-primary) hover:bg-(--color-primary-hover) text-(--color-button-text) px-4 py-2 rounded-lg transition active:scale-[0.98]"
        >
          sign up
        </button>

        {/* Divider */}
        <hr className="w-full border-(--color-border)" />

        {/* Footer Link */}
        <p className="text-sm text-(--color-text-muted)">
          already have an account?{" "}
          <button className="text-(--color-primary) hover:underline bg-transparent border-none p-0 cursor-pointer">
            sign in
          </button>
        </p>
      </div>
    </div>
  );
}

export default SignUp;
