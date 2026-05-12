import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import ThemeToggle from "./ThemeToggle";
import BackButton from "./BackButton";
import Empty from "./components/Empty";

export default function ProfilePage() {
  const [user] = useState(() => {
    const stored = localStorage.getItem("user");
    return stored ? JSON.parse(stored) : null;
  });
  const [theme, setTheme] = useState("dark");
  const [tab, setTab] = useState("info");
  const [isUpdated, setIsUpdated] = useState(false);

  const [name, setName] = useState(user?.name || "");
  const [email, setEmail] = useState(user?.email || "");
  const [Secrets, setSecrets] = useState<any[]>([]);
  const [containers, setContainers] = useState<any[]>([]);

  const [newSecretName, setNewSecretName] = useState("");
  const [newSecretValue, setNewSecretValue] = useState("");

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    authCheck();
    document.documentElement.dataset.theme = theme;
    getuserData();
  }, [theme]);

  function authCheck(): boolean {
    const user = localStorage.getItem("user");
    if (!user) {
      window.location.href = "/login";
    }
    return true;
  }

  function getuserData() {
    fetch(`http://localhost:8080/api/profile?userId=${user.id}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    })
      .then((res) => res.json())
      .then((data) => {
        console.log(data);
        setSecrets(data.secrets);
        setContainers(data.containers);
      })
      .catch((error) => {
        console.error("Error:", error);
      });
  }

  function addNewSecret() {
    fetch("http://localhost:8080/api/addSecret", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        userId: user.id,
        name: newSecretName,
        value: newSecretValue,
      }),
    })
      .then((response) => response.json())
      .then((data) => {
        setSecrets([
          ...Secrets,
          { name: newSecretName, value: newSecretValue },
        ]);
      })
      .catch((error) => {
        console.error("Error:", error);
      });
  }

  function deleteContainer(id: string) {
    fetch(`http://localhost:8080/api/deleteContainer`, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        containerId: id,
      }),
    })
      .then((res) => res.json())
      .then((data) => {
        console.log(data);
      })
      .catch((error) => {
        console.error("Error:", error);
      });
  }

  function deleteSecret(id: string) {
    fetch(`http://localhost:8080/api/deleteSecret`, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        secretId: id,
      }),
    })
      .then((res) => res.json())
      .then((data) => {
        console.log(data);
      })
      .catch((error) => {
        console.error("Error:", error);
      });
  }

  return (
    <div className="relative flex flex-col items-center justify-center min-h-screen w-screen p-6 overflow-hidden bg-(--color-bg-main) text-(--color-text-main)">
      {/* Grid texture */}
      <div className="absolute inset-0 pointer-events-none opacity-60 grid-texture" />

      {/* Cyan orb */}
      <div className="absolute pointer-events-none rounded-full cyan-orb" />

      {/* Back button */}
      <div className="relative z-10 flex justify-between w-full max-w-lg mb-4">
        <BackButton />
        <ThemeToggle />
      </div>

      {/* Main card */}
      <div className="relative z-10 flex flex-col gap-5 w-full max-w-lg bg-(--color-bg-card) border border-(--color-border-strong) rounded-2xl p-7 shadow-card">
        {/* Profile header */}
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-(--color-primary-light) border border-(--color-border-accent) flex items-center justify-center shrink-0">
            <span className="font-mono text-lg font-semibold text-(--color-primary)">
              {user?.name?.charAt(0).toUpperCase() || "U"}
            </span>
          </div>
          <div>
            <p className="text-sm font-semibold tracking-tight m-0">
              {user?.name}
            </p>
            <p className="font-mono text-[11px] text-(--color-text-muted) m-0">
              {user?.email}
            </p>
          </div>
        </div>

        {/* Divider */}
        <div className="h-px bg-(--color-border) -mx-7" />

        {/* Tab toggle */}
        <div className="relative flex w-full items-center rounded-[10px] bg-(--color-bg-muted) border border-(--color-border) p-1">
          <div
            className={`absolute inset-y-1 left-1 w-[calc(50%-4px)] rounded-[7px] bg-(--color-bg-card) border border-(--color-border-strong) shadow-sm transition-all duration-300 ease-in-out ${
              tab === "settings" ? "translate-x-full" : ""
            }`}
          />
          {["info", "settings"].map((t) => (
            <label
              key={t}
              className={`font-mono relative z-10 flex flex-1 cursor-pointer items-center justify-center py-1.5 text-xs tracking-wider transition-colors duration-200 ${
                tab === t
                  ? "text-(--color-text-main) font-semibold"
                  : "text-(--color-text-muted)"
              }`}
            >
              <input
                type="radio"
                className="sr-only"
                checked={tab === t}
                onChange={() => {
                  setTab(t);
                  setIsUpdated(false);
                }}
              />
              {t}
            </label>
          ))}
        </div>

        {/* Info tab */}
        {tab === "info" && (
          <div className="flex flex-col gap-5">
            <p className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest m-0">
              Profile Info
            </p>

            {/* Username + Email */}
            {[
              { label: "Username", value: name, setter: setName, type: "text" },
              { label: "Email", value: email, setter: setEmail, type: "text" },
            ].map(({ label, value, setter, type }) => (
              <div key={label} className="flex flex-col gap-1.5">
                <label className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest">
                  {label}
                </label>
                <input
                  type={type}
                  value={value}
                  onChange={(e) => {
                    setter(e.target.value);
                    setIsUpdated(true);
                  }}
                  className="font-mono w-full px-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) focus:border-(--color-border-accent) transition"
                />
              </div>
            ))}

            {/* Secrets */}
            <div className="flex flex-col gap-2">
              <label className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest">
                Secrets
              </label>

              {Secrets.map((Secret) => (
                <div key={Secret.id} className="flex gap-2">
                  <input
                    type="text"
                    value={Secret.name || ""}
                    onChange={() => setIsUpdated(true)}
                    placeholder="key"
                    className="font-mono w-1/3 px-3 py-2 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
                  />
                  <input
                    type="text"
                    value={Secret.value || ""}
                    onChange={() => setIsUpdated(true)}
                    placeholder="value"
                    className="font-mono flex-1 px-3 py-2 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
                  />
                  <button
                    onClick={() => deleteSecret(Secret.id)}
                    className="font-mono px-3 py-2 bg-(--color-error-bg) text-(--color-error) border border-(--color-error-border) rounded-lg text-[13px] font-semibold transition hover:opacity-90 active:scale-[0.98]"
                  >
                    −
                  </button>
                </div>
              ))}

              {/* New secret row */}
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="key"
                  onChange={(e) => setNewSecretName(e.target.value)}
                  className="font-mono w-1/3 px-3 py-2 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
                />
                <input
                  type="text"
                  placeholder="value"
                  onChange={(e) => setNewSecretValue(e.target.value)}
                  className="font-mono flex-1 px-3 py-2 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
                />
                <button
                  onClick={addNewSecret}
                  className="font-mono px-3 py-2 bg-(--color-primary-light) text-(--color-primary) border border-(--color-border-accent) rounded-lg text-[13px] font-semibold transition hover:bg-(--color-primary-glow) active:scale-[0.98]"
                >
                  +
                </button>
              </div>
            </div>

            {/* Containers */}
            <div className="flex flex-col gap-2">
              <label className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest">
                Containers
              </label>
              {containers.length === 0 ? (
                <Empty label="no containers found" />
              ) : (
                containers.map((container) => (
                  <div key={container.id} className="flex gap-2">
                    <div className="font-mono flex-1 px-3 py-2 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-muted)">
                      {container.name || ""}
                    </div>
                    <button
                      onClick={() => deleteContainer(container.id)}
                      className="font-mono px-4 py-2 bg-(--color-error-bg) text-(--color-error) border border-(--color-error-border) rounded-lg text-[13px] font-semibold transition hover:opacity-90 active:scale-[0.98]"
                    >
                      delete
                    </button>
                  </div>
                ))
              )}
            </div>

            {isUpdated && (
              <button className="font-mono w-full py-2.5 bg-(--color-primary) text-(--color-button-text) rounded-lg text-[13px] font-semibold tracking-wider transition active:scale-[0.985] hover:opacity-90">
                update info →
              </button>
            )}
          </div>
        )}

        {/* Settings tab */}
        {tab === "settings" && (
          <div className="flex flex-col gap-4">
            <p className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest m-0">
              Change Password
            </p>

            {[
              { placeholder: "new password", setter: setPassword },
              { placeholder: "confirm password", setter: setConfirmPassword },
            ].map(({ placeholder, setter }) => (
              <input
                key={placeholder}
                type="password"
                placeholder={placeholder}
                className="font-mono w-full px-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) focus:border-(--color-border-accent) transition"
                onChange={(e) => {
                  setter(e.target.value);
                  setIsUpdated(true);
                }}
              />
            ))}

            {isUpdated && (
              <button className="font-mono w-full py-2.5 bg-(--color-primary) text-(--color-button-text) rounded-lg text-[13px] font-semibold tracking-wider transition active:scale-[0.985] hover:opacity-90">
                update password →
              </button>
            )}
          </div>
        )}

        {/* Divider */}
        <div className="h-px bg-(--color-border) -mx-7" />

        {/* Logout */}
        <button className="font-mono w-full py-2.5 bg-(--color-error-bg) text-(--color-error) border border-(--color-error-border) rounded-lg text-[13px] font-semibold tracking-wider transition hover:opacity-90 active:scale-[0.98]">
          logout
        </button>
      </div>
    </div>
  );
}
