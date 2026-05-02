import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

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
    <div className="flex flex-col items-center justify-center bg-(--color-bg-main) text-(--color-text-main) min-h-screen w-screen p-4">
      <div className="flex flex-col items-center gap-6 bg-(--color-bg-card) p-10 rounded-2xl shadow-lg w-full max-w-md">
        <Link
          to="/"
          className="flex absolute top-5 left-5 font-black bg-(--color-bg-muted) px-4 py-2 rounded-full hover:bg-(--color-bg-card) hover:border-2 border-(--color-border) transition-all duration-150"
        >
          back
        </Link>
        {/* Profile Header */}
        <div className="flex flex-col items-center gap-3">
          <div className="w-20 h-20 rounded-full bg-(--color-bg-muted) flex items-center justify-center text-2xl font-bold">
            {user?.name?.charAt(0).toUpperCase() || "U"}
          </div>
          <h1 className="text-xl font-semibold">{user?.name}</h1>
          <p className="text-sm text-(--color-text-muted)">{user?.email}</p>
        </div>

        {/* Toggle Tabs */}
        <div className="relative flex w-full items-center rounded-full bg-(--color-bg-main) p-1 border border-(--color-border)">
          <div
            className={`absolute inset-y-1 left-1 w-[calc(50%-4px)] rounded-full bg-(--color-bg-card) shadow transition-all duration-300 ease-in-out ${
              tab === "settings" ? "translate-x-full" : ""
            }`}
          />

          <label className="relative z-10 flex flex-1 cursor-pointer items-center justify-center py-2 text-sm font-medium">
            <input
              type="radio"
              className="sr-only"
              checked={tab === "info"}
              onChange={() => (setTab("info"), setIsUpdated(false))}
            />
            Info
          </label>

          <label className="relative z-10 flex flex-1 cursor-pointer items-center justify-center py-2 text-sm font-medium">
            <input
              type="radio"
              className="sr-only"
              checked={tab === "settings"}
              onChange={() => (setTab("settings"), setIsUpdated(false))}
            />
            Settings
          </label>
        </div>

        {/* Info Section */}
        {tab === "info" && (
          <div className="flex flex-col gap-4 w-full">
            <h2 className="text-lg font-semibold tracking-tight">
              Profile Info
            </h2>

            <div className="flex flex-col gap-2">
              <label className="text-sm text-(--color-text-muted)">
                Username
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => (setName(e.target.value), setIsUpdated(true))}
                className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted)"
              />
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-sm text-(--color-text-muted)">Email</label>
              <input
                type="text"
                value={email}
                onChange={(e) => (setEmail(e.target.value), setIsUpdated(true))}
                className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted)"
              />
            </div>

            {/* Secrets */}
            <div className="flex flex-col gap-2">
              <label className="text-sm text-(--color-text-muted)">
                Secrets
              </label>
              {Secrets.map((Secret) => (
                <div className="flex gap-4" key={Secret.id}>
                  <input
                    type="text"
                    value={Secret.name || ""}
                    onChange={(e) => setIsUpdated(true)}
                    className="w-1/3 px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted)"
                  />
                  <input
                    type="text"
                    value={Secret.value || ""}
                    onChange={(e) => setIsUpdated(true)}
                    className="px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted)"
                  />
                  <button
                    onClick={() => deleteSecret(Secret.id)}
                    className="bg-(--color-error) hover:opacity-90 text-white px-4 py-2 rounded-lg transition"
                  >
                    -
                  </button>
                </div>
              ))}
              <div className="flex gap-4">
                <input
                  type="text"
                  onChange={(e) => setNewSecretName(e.target.value)}
                  className="w-1/3 px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted)"
                />
                <input
                  type="text"
                  onChange={(e) => setNewSecretValue(e.target.value)}
                  className="px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted)"
                />
                <button
                  onClick={addNewSecret}
                  className="bg-(--color-primary) hover:bg-(--color-primary-hover) text-(--color-button-text) px-4 py-2 rounded-lg transition active:scale-[0.98]"
                >
                  +
                </button>
              </div>
            </div>

            {/* containers */}
            <div className="flex flex-col gap-2">
              <label className="text-sm text-(--color-text-muted)">
                Containers
              </label>
              {containers.map((container) => (
                <div key={container.id} className="flex gap-4">
                  <label className="w-2/3 px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-muted) text-center">
                    {container.name || ""}
                  </label>
                  <button
                    onClick={() => deleteContainer(container.id)}
                    className="w-1/3 bg-(--color-error) hover:opacity-90 text-white px-4 py-2 rounded-lg transition"
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>

            {isUpdated && (
              <button className="w-full bg-(--color-primary) hover:bg-(--color-primary-hover) text-(--color-button-text) px-4 py-2 rounded-lg transition active:scale-[0.98]">
                Update Info
              </button>
            )}
          </div>
        )}

        {/* Settings Section */}
        {tab === "settings" && (
          <div className="flex flex-col gap-4 w-full">
            <h2 className="text-lg font-semibold tracking-tight">Settings</h2>

            <input
              type="password"
              placeholder="New Password"
              className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-card) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
              onChange={(e) => (
                setPassword(e.target.value),
                setIsUpdated(true)
              )}
            />

            <input
              type="password"
              placeholder="Confirm Password"
              className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-card) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
              onChange={(e) => (
                setConfirmPassword(e.target.value),
                setIsUpdated(true)
              )}
            />

            {isUpdated && (
              <button className="w-full bg-(--color-primary) hover:bg-(--color-primary-hover) text-(--color-button-text) px-4 py-2 rounded-lg transition active:scale-[0.98]">
                Update Password
              </button>
            )}
          </div>
        )}

        {/* Divider */}
        <hr className="w-full border-(--color-border)" />

        {/* Logout */}
        <button className="w-full bg-(--color-error) hover:opacity-90 text-white px-4 py-2 rounded-lg transition">
          Logout
        </button>
      </div>
    </div>
  );
}
