import { useEffect, useRef, useState } from "react";
import Auth from "./Auth";

import ThemeToggle from "./ThemeToggle";
import useTheme from "./hooks/useTheme";
import Empty from "./components/Empty";
import TaskPanel from "./TaskPanel";

function App() {
  const { theme, toggleTheme } = useTheme();
  const [mode, setMode] = useState("resources");

  const [memory, setMemory] = useState("0");
  const [cpu, setCpu] = useState("0");

  const [containerIsRunning, setContainerIsRunning] = useState(false);
  const [containerId, setContainerId] = useState("");
  const [containersList, setContainersList] = useState([
    { name: "container name", id: "container_id" },
  ]);

  const [selectedContainer, setSelectedContainer] = useState("");

  const [command, setCommand] = useState("");
  const [output, setOutput] = useState("");
  const [provider, setProvider] = useState("ollama");

  const terminalRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (terminalRef.current) {
      terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
    }
  }, [output]);

  function authCheck(): boolean {
    const user = localStorage.getItem("user");
    if (!user) {
      window.location.href = "/login";
    }
    return true;
  }

  function startStatusStream(id: string) {
    setTimeout(() => {
      const eventSource = new EventSource(
        `http://localhost:8080/api/container-status/${id}`,
      );

      eventSource.addEventListener("stopped", () => {
        setContainerIsRunning(false);
        setContainerId("");
        setOutput("");
        eventSource.close();
      });

      eventSource.onerror = (e) => {
        console.error("SSE error:", e);
        if (eventSource.readyState === EventSource.CLOSED) {
          eventSource.close();
        }
      };
    }, 500);
  }

  // Function to run the container
  function RunContainer() {
    authCheck();
    if (containerIsRunning) {
      return alert("A container is already running.");
    }

    console.log("Memory:", memory);
    console.log("CPU:", cpu);
    console.log("User: ", localStorage.getItem("user"));

    const parsedMemory = parseFloat(memory);
    const parsedCpu = parseFloat(cpu) / 100;
    const userRaw = localStorage.getItem("user");
    const user = userRaw ? JSON.parse(userRaw) : null;

    //input validation
    if (isNaN(parsedMemory) || isNaN(parsedCpu)) {
      return alert("Please enter valid numbers for memory and CPU.");
    } else if (parsedMemory < 0 || parsedCpu < 0) {
      return alert("Please enter non-negative numbers for memory and CPU.");
    } else if (parsedMemory < 6) {
      return alert("Please enter valid numbers for memory and CPU.");
    }

    // Make a POST request to the backend
    fetch("http://localhost:8080/api/run", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ memory, cpu, user }),
    })
      .then((res) => res.json())
      .then((data) => {
        console.log("Response:", data);
        setContainerIsRunning(true);
        setContainerId(data.containerId);
        startStatusStream(data.containerId);
      })
      .catch((error) => console.error("Error:", error));
  }

  // Function to stop the container
  function StopContainer() {
    setOutput((prev) => prev + "closing container...\n");

    fetch("http://localhost:8080/api/stop", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ containerId }),
    })
      .then((res) => res.json())
      .then(
        (data) => (
          console.log("Response:", data),
          setContainerIsRunning(false),
          setContainerId(""),
          setOutput("")
        ),
      )
      .catch((error) => console.error("Error:", error));
  }

  // Function to execute commands in the container
  function ExecCommands() {
    setOutput((prev) => prev + ">" + command + "\n");

    const userRaw = localStorage.getItem("user");
    if (!userRaw) {
      setOutput((prev) => prev + "Please log in first.\n");
      return;
    }

    const user = JSON.parse(userRaw);
    if (!user.id) {
      setOutput((prev) => prev + "Please log in first.\n");
      return;
    }

    if (command === "cl" || command === "clear") {
      setOutput("");
      return;
    }

    fetch("http://localhost:8080/api/exec", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        command,
        containerId,
        userId: user.id,
        provider: command.startsWith("/ask") ? provider : undefined,
      }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.type === "ai") {
          setOutput((prev) => prev + data.response + "\n");
          return;
        }

        const commandId = data.commandId;
        let received = false;

        const eventSource = new EventSource(
          `http://localhost:8080/api/stream/${commandId}`,
        );

        const timeout = setTimeout(() => {
          if (!received) {
            eventSource.close();
            setOutput((prev) => prev + "Command timed out\n");
          }
        }, 10000);

        eventSource.onmessage = (event) => {
          setOutput((prev) => prev + event.data + "\n");
          eventSource.close();
          received = true;
          clearTimeout(timeout);
        };

        eventSource.onerror = () => {
          if (!received) {
            eventSource.close();
            clearTimeout(timeout);
            setOutput((prev) => prev + "Error receiving output\n");
          }
        };
      })
      .catch((error) => console.error("Error:", error));
  }

  // Function to get the list of containers
  function ListContainers() {
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    if (!user || !user.id) {
      return;
    }

    fetch(`http://localhost:8080/api/listContainers?userId=${user.id}`, {
      method: "GET",
    })
      .then((res) => res.json())
      .then((data) => {
        console.log("Response:", data);
        const merged = data.containerList.map(
          (c: { name: string; id: string }) => ({
            name: c.name,
            id: c.id,
          }),
        );
        setContainersList(merged);
        if (merged.length > 0) setSelectedContainer(merged[0].id);
      })
      .catch((error) => console.error("Error:", error));
  }

  // Function to start the container
  function StartContainer() {
    authCheck();
    if (containerIsRunning) {
      return alert("A container is already running.");
    }

    console.log("Selected container:", selectedContainer);

    fetch("http://localhost:8080/api/start", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ containerId: selectedContainer }),
    })
      .then((res) => res.json())
      .then(
        (data) => (
          console.log("Response:", data),
          setContainerIsRunning(true),
          setContainerId(selectedContainer),
          startStatusStream(selectedContainer)
        ),
      )
      .catch((error) => console.error("Error:", error));
  }

  function handleFileUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    fetch(`http://localhost:8080/api/uploadFile?containerId=${containerId}`, {
      method: "POST",
      body: formData,
    })
      .then((res) => res.json())
      .then((data) => console.log("Response:", data))
      .catch((error) => console.error("Error:", error));
  }

  return (
    <div className="relative flex flex-col items-center justify-center min-h-screen w-screen p-6 overflow-hidden bg-(--color-bg-main) text-(--color-text-main)">
      {/* Grid texture */}
      <div className="absolute inset-0 pointer-events-none opacity-60 grid-texture" />

      {/* Cyan orb */}
      <div className="absolute pointer-events-none rounded-full cyan-orb" />

      {/* Theme toggle */}
      <div className="relative z-10 flex justify-end w-full max-w-sm mb-4">
        <ThemeToggle />
      </div>

      <Auth />

      {/* Layout — centers when solo, two-col when panel is open */}
      <div
        className={`relative z-10 flex items-start gap-4 w-full transition-all duration-300 ${containerIsRunning ? "max-w-4xl justify-start" : "max-w-sm justify-center"}`}
      >
        {/* Main card */}
        <div
          className="flex flex-col gap-5 w-full max-w-sm bg-(--color-bg-card) border border-(--color-border-strong) rounded-2xl p-7 shrink-0"
          style={{
            boxShadow:
              theme === "dark"
                ? "0 0 0 1px rgba(0,212,255,0.05), 0 24px 48px rgba(0,0,0,0.5)"
                : "0 4px 24px rgba(0,0,0,0.08)",
          }}
        >
          {/* Header */}
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-(--color-primary-light) border border-(--color-border-accent) flex items-center justify-center">
              <span className="text-sm text-(--color-primary)">⬡</span>
            </div>
            <div>
              <p className="text-sm font-semibold m-0 tracking-tight">
                Container Manager
              </p>
              <p className="font-mono text-[11px] text-(--color-text-muted) m-0">
                runtime control
              </p>
            </div>
          </div>

          {/* Divider */}
          <div className="h-px bg-(--color-border) -mx-7" />

          {/* Mode toggle */}
          <div className="relative flex w-full items-center rounded-[10px] bg-(--color-bg-muted) border border-(--color-border) p-1">
            <div
              className={`absolute inset-y-1 left-1 w-[calc(50%-4px)] rounded-[7px] bg-(--color-bg-card) border border-(--color-border-strong) shadow-sm transition-all duration-300 ease-in-out ${
                mode === "existing" ? "translate-x-full" : ""
              }`}
            />
            {["resources", "existing"].map((m) => (
              <label
                key={m}
                className={`font-mono relative z-10 flex flex-1 cursor-pointer items-center justify-center py-1.5 text-xs tracking-wider transition-colors duration-200 ${
                  mode === m
                    ? "text-(--color-text-main) font-semibold"
                    : "text-(--color-text-muted)"
                }`}
              >
                <input
                  type="radio"
                  name="slider"
                  className="sr-only"
                  checked={mode === m}
                  onChange={() => {
                    setMode(m);
                    if (m === "existing") ListContainers();
                  }}
                />
                {m}
              </label>
            ))}
          </div>

          {/* Resources mode */}
          {mode === "resources" && (
            <div className="flex flex-col gap-3">
              <p className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest m-0">
                Allocate Resources
              </p>
              {[
                {
                  placeholder: "Memory limit (MB)",
                  setter: setMemory,
                  icon: "▦",
                },
                { placeholder: "CPU ceiling (%)", setter: setCpu, icon: "◈" },
              ].map(({ placeholder, setter, icon }) => (
                <div key={placeholder} className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[13px] text-(--color-primary) pointer-events-none">
                    {icon}
                  </span>
                  <input
                    type="number"
                    placeholder={placeholder}
                    onChange={(e) => setter(e.target.value)}
                    className="font-mono w-full pl-9 pr-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) focus:border-(--color-border-accent) transition"
                  />
                </div>
              ))}
              <button
                onClick={RunContainer}
                className="font-mono w-full py-2.5 bg-(--color-primary) text-(--color-button-text) rounded-lg text-[13px] font-semibold tracking-wider transition active:scale-[0.985] hover:opacity-90"
              >
                run container →
              </button>
            </div>
          )}

          {/* Existing mode */}
          {mode === "existing" && (
            <div className="flex flex-col gap-3">
              <p className="font-mono text-[11px] text-(--color-text-muted) uppercase tracking-widest m-0">
                Select Container
              </p>
              {containersList.length === 0 ? (
                <Empty label="no containers found" />
              ) : (
                <select
                  value={selectedContainer}
                  onChange={(e) => setSelectedContainer(e.target.value)}
                  className="font-mono w-full px-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
                >
                  {containersList.map((container) => (
                    <option key={container.id} value={container.id}>
                      {container.name}
                    </option>
                  ))}
                </select>
              )}
              <button
                onClick={StartContainer}
                className="font-mono w-full py-2.5 bg-(--color-primary) text-(--color-button-text) rounded-lg text-[13px] font-semibold tracking-wider transition active:scale-[0.985] hover:opacity-90"
              >
                start container →
              </button>
            </div>
          )}
        </div>

        {/* Running container panel */}
        {containerIsRunning && (
          <div className="animate-slide-in flex flex-col gap-4 flex-1 bg-(--color-bg-card) border border-(--color-border-accent) rounded-2xl px-8 py-6 shadow-panel">
            {/* Status bar */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-[7px] h-[7px] rounded-full bg-(--color-success) inline-block animate-pulse" />
                <span className="font-mono text-[12px] text-(--color-success) font-medium">
                  running
                </span>
                <span className="flex flex-row gap-1 font-mono text-[11px] text-(--color-text-muted)">
                  id:{" "}
                  <span className="text-(--color-primary)">{containerId}</span>
                </span>
              </div>
            </div>

            {/* Terminal output */}
            <textarea
              ref={terminalRef}
              readOnly
              value={output}
              className="font-mono w-full h-56 p-4 rounded-lg text-[13px] leading-relaxed resize-none outline-none overflow-y-auto bg-[#050810] border border-[rgba(0,212,255,0.1)] text-[#00d4ff]"
            />

            {/* Command row */}
            <div className="flex gap-3">
              <input
                type="text"
                placeholder="$ enter command..."
                onChange={(e) => setCommand(e.target.value)}
                className="font-mono flex-1 px-4 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) focus:border-(--color-border-accent) transition"
              />
              <label className="font-mono w-6 py-3  text-(--color-text-muted) text-[13px] font-semibold tracking-wider transition hover:bg-(--color-bg-card) active:scale-[0.98] cursor-pointer text-center">
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M21.44 11.05l-9.19 9.19a6 6 0 01-8.49-8.49l9.19-9.19a4 4 0 015.66 5.66l-9.2 9.19a2 2 0 01-2.83-2.83l8.49-8.48" />
                </svg>
                <input
                  type="file"
                  className="sr-only"
                  onChange={handleFileUpload}
                />
              </label>
              <select
                onChange={(e) => setProvider(e.target.value)}
                className="font-mono w-[110px] px-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
              >
                <option value="ollama">ollama</option>
                <option value="gemini">gemini</option>
              </select>
            </div>

            {/* Action buttons */}
            <div className="flex gap-3">
              <button
                onClick={ExecCommands}
                className="font-mono flex-1 py-3 bg-(--color-primary-light) text-(--color-primary) border border-(--color-border-accent) rounded-lg text-[13px] font-semibold tracking-wider transition hover:bg-(--color-primary-glow) active:scale-[0.98]"
              >
                exec
              </button>
              <button
                onClick={StopContainer}
                className="font-mono flex-1 py-3 bg-(--color-error-bg) text-(--color-error) border border-(--color-error-border) rounded-lg text-[13px] font-semibold tracking-wider transition hover:opacity-90 active:scale-[0.98]"
              >
                stop
              </button>
            </div>
          </div>
        )}
      </div>
      {/* Tasks section */}
      <TaskPanel />
    </div>
  );
}

export default App;
