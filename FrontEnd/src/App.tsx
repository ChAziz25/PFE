import { useEffect, useState } from "react";
import Auth from "./Auth";

import ollama from "./assets/ollama.svg";
import gemini from "./assets/gemini.svg";

function App() {
  const [theme, setTheme] = useState("dark");
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

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

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

  function StartContainerStream(requestId: string) {
    const eventSource = new EventSource(
      `http://localhost:8080/api/containers/stream?requestId=${requestId}`,
    );

    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);

      console.log("Container Ready:", data);

      setContainerId(data.containerId);
      startStatusStream(data.containerId);

      eventSource.close();
    };

    eventSource.onerror = (err) => {
      console.log("SSE error:", err);
      eventSource.close();
    };
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
        StartContainerStream(data.requestedId);
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

  return (
    <div className="flex flex-col items-center justify-center bg-(--color-bg-main) text-(--color-text-main) min-h-screen w-screen p-4">
      <Auth />
      <div className="flex flex-col items-center gap-6 bg-(--color-bg-card) p-10 rounded-2xl shadow-lg w-full max-w-md">
        {/* Toggle Section */}
        <div className="flex flex-col items-center gap-6 bg-(--color-bg-muted) p-6 rounded-xl w-full">
          {/* Slider */}
          <div className="relative flex w-full items-center rounded-full bg-(--color-bg-main) p-1 border border-(--color-border)">
            <div
              className={`absolute inset-y-1 left-1 w-[calc(50%-4px)] rounded-full bg-(--color-bg-card) shadow transition-all duration-300 ease-in-out ${
                mode === "existing" ? "translate-x-full" : ""
              }`}
            />

            <label className="relative z-10 flex flex-1 cursor-pointer items-center justify-center py-2 text-sm font-medium">
              <input
                type="radio"
                name="slider"
                className="sr-only"
                checked={mode === "resources"}
                onChange={() => setMode("resources")}
              />
              resources
            </label>

            <label className="relative z-10 flex flex-1 cursor-pointer items-center justify-center py-2 text-sm font-medium">
              <input
                type="radio"
                name="slider"
                className="sr-only"
                checked={mode === "existing"}
                onChange={() => (setMode("existing"), ListContainers())}
              />
              existing
            </label>
          </div>

          {/* Resources Mode */}
          {mode === "resources" && (
            <div className="flex flex-col gap-4 w-full">
              <h1 className="text-lg font-semibold tracking-tight">
                Resources
              </h1>

              <input
                type="number"
                placeholder="Memory (MB)"
                onChange={(e) => setMemory(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-card) text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
              />

              <input
                type="number"
                placeholder="CPU (%)"
                onChange={(e) => setCpu(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-card) text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
              />

              <button
                onClick={RunContainer}
                className="w-full bg-(--color-primary) hover:bg-(--color-primary-hover) text-(--color-button-text) px-4 py-2 rounded-lg transition active:scale-[0.98]"
              >
                run
              </button>
            </div>
          )}

          {/* Existing Mode */}
          {mode === "existing" && (
            <div className="flex flex-col gap-4 w-full">
              <h1 className="text-lg font-semibold tracking-tight">
                Existing Containers
              </h1>

              <select
                value={selectedContainer}
                onChange={(e) => setSelectedContainer(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-card) text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
              >
                {containersList.map((container) => (
                  <option key={container.id} value={container.id}>
                    {container.name}
                  </option>
                ))}
              </select>

              <button
                onClick={StartContainer}
                className="w-full bg-(--color-primary) hover:bg-(--color-primary-hover) text-(--color-button-text) px-4 py-2 rounded-lg transition active:scale-[0.98]"
              >
                start
              </button>
            </div>
          )}
        </div>

        {/* Divider */}
        <hr className="w-full border-(--color-border)" />

        {/* Running Container Panel */}
        {containerIsRunning && (
          <div className="w-full bg---color-bg-muted border border-(--color-border) rounded-xl p-4 flex flex-col gap-3">
            <h2 className="text-sm font-semibold text-(--color-success)">
              ● Container {containerId} Running
            </h2>

            <textarea
              readOnly
              value={output}
              className="w-full h-40 p-3 rounded-lg bg-black text-green-400 font-mono text-sm border border-(--color-border)"
            />

            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Enter command..."
                onChange={(e) => setCommand(e.target.value)}
                className="w-3/4 px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-card) text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
              />
              <select
                onChange={(e) => setProvider(e.target.value)}
                className="w-1/4 px-3 py-2 rounded-lg border border-(--color-border) bg-(--color-bg-card) text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
              >
                <option value="ollama">Ollama</option>
                <option value="gemini">Gemini</option>
              </select>
            </div>

            <div className="flex gap-2">
              <button
                onClick={ExecCommands}
                className="flex-1 bg-(--color-primary) hover:bg-(--color-primary-hover) text-(--color-button-text) px-4 py-2 rounded-lg transition active:scale-[0.98]"
              >
                exec
              </button>

              <button
                onClick={StopContainer}
                className="flex-1 bg-(--color-error) hover:opacity-90 text-white px-4 py-2 rounded-lg transition"
              >
                stop
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
