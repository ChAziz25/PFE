const express = require("express");
const cors = require("cors");
const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

const containers = new Map();

// Function to run the container and return its ID
app.post("/run", (req, res) => {
  const { spawn } = require("child_process");

  const memory = req.body.memory;
  const cpu = req.body.cpu;

  try {
    const process = spawn("docker", [
      "run",
      "-d",
      "-p",
      "8080:8080",
      `--memory=${memory}mb`,
      `--cpus=${cpu}`,
      "pfe-sandbox",
    ]);

    let containerId = "";

    let output = "";

    process.stdout.on("data", (data) => {
      containerId += data.toString();
      containerId = containerId.trim();
    });

    process.stderr.on("data", (data) => {
      output += data.toString();
      output = output.trim();
    });

    process.on("close", () => {
      if (output !== "") {
        return res.status(500).json({
          message: output,
        });
      } else {
        const logs = spawn("docker", ["logs", "-f", containerId]);

        let buffer = "";

        logs.stdout.on("data", (data) => {
          buffer += data.toString();

          console.log("[CONTAINER LOG]", data.toString());

          if (buffer.includes("READY")) {
            console.log("Container is ready");
            logs.kill();
          }
        });

        logs.stderr.on("data", (data) => {
          console.error("[CONTAINER ERROR]", data.toString());
        });

        containers.set(containerId, {
          containerId,
          lastSeen: Date.now(),
        });

        return res.status(200).json({
          message: "container started",
          containerId,
        });
      }
    });
  } catch (error) {
    return res.status(500).json({
      message: "error",
    });
  }
});

// Function to stop the container
app.post("/stop", (req, res) => {
  const { spawn } = require("child_process");

  const containerId = req.body.containerId;

  try {
    const process = spawn("docker", ["stop", containerId]);

    process.on("close", () => {
      containers.delete(containerId);

      return res.status(200).json({
        message: "container stopped",
      });
    });
  } catch (error) {
    return res.status(500).json({
      message: "error",
    });
  }
});

// Function to execute commands in the container
app.post("/exec", async (req, res) => {
  const { command } = req.body;
  const port = 8080;

  try {
    const response = await fetch(`http://127.0.0.1:${port}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ command }),
    });

    const data = await response.json();

    res.json(data);
  } catch (err) {
    res.status(500).json({ error: "execution failed" });
  }
});

// Function to get the list of containers
app.get("/listContainers", (req, res) => {
  const { spawn } = require("child_process");
  const listContainersCommand = `docker ps -a`;

  try {
    const process = spawn("bash", ["-c", listContainersCommand]);

    let output = "";
    let containerList = [];
    let containerListId = [];

    process.stdout.on("data", (data) => {
      output += data.toString();
      output = output.trim();

      output.split("\n").forEach((container) => {
        if (container.includes("pfe-sandbox")) {
          parts = container.split(" ");
          containerList.push(parts[parts.length - 1]);
          containerListId.push(parts[0]);
        }
      });
    });

    process.stderr.on("data", (data) => {
      output += data.toString();
      output = output.trim();
    });

    process.on("close", (code) => {
      if (code !== 0) {
        return res.status(500).json({
          message: output,
        });
      } else {
        return res.status(200).json({
          message: "success",
          containerList,
          containerListId,
        });
      }
    });
  } catch (error) {
    return res.status(500).json({
      message: "error",
    });
  }
});

app.post("/startContainer", (req, res) => {
  const { spawn } = require("child_process");

  const containerName = req.body.selectedContainer;

  try {
    const process = spawn("docker", ["start", containerName]);

    process.on("close", () => {
      containers.set(containerName, {
        containerName: containerName,
        lastSeen: Date.now(),
      });

      return res.status(200).json({
        message: "container started",
      });
    });
  } catch (error) {
    return res.status(500).json({
      message: "error",
    });
  }
});

app.post("/heartbeat", express.json(), (req, res) => {
  const { containerId } = req.body;

  const container = containers.get(containerId);

  if (container) {
    container.lastSeen = Date.now();
  }

  res.sendStatus(200);
});

setInterval(async () => {
  const now = Date.now();

  for (const [id, c] of containers.entries()) {
    if (now - c.lastSeen > 20000) {
      const { spawn } = require("child_process");
      console.log("Auto-stopping:", id);

      try {
        const process = spawn("docker", ["stop", id]);
        process.on("exit", () => {
          containers.delete(id);
        });
      } catch (err) {
        console.error(err);
      }
    }
  }
}, 5000);

app.listen(port, () => {
  console.log(`Example app listening on port ${port}`);
});
