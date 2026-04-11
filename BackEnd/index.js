const express = require("express");
const cors = require("cors");
const Redis = require("ioredis");
const { Kafka } = require("kafkajs");
const { PrismaClient } = require("@prisma/client");
const Minio = require("minio");

const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

const redis = new Redis({ host: "localhost", port: 6379 });
redis.on("connect", () => console.log("✅ Connected to Redis"));
redis.on("error", (err) => console.error("Redis error:", err));

const kafka = new Kafka({ clientId: "sandbox", brokers: ["localhost:9092"] });
const producer = kafka.producer();
const consumer = kafka.consumer({ groupId: "sandbox-results" });

const pendingResults = new Map();

const prisma = new PrismaClient();

const minioClient = new Minio.Client({
  endPoint: "localhost",
  port: 9000,
  useSSL: false,
  accessKey: "sandbox",
  secretKey: "sandbox123",
});

async function startKafka() {
  await producer.connect();
  await consumer.connect();
  await consumer.subscribe({ topic: "results", fromBeginning: false });

  await consumer.run({
    eachMessage: async ({ message }) => {
      const { commandId, output } = JSON.parse(message.value.toString());
      const resolve = pendingResults.get(commandId);
      if (resolve) {
        resolve(output);
        pendingResults.delete(commandId);
      }
    },
  });

  console.log("✅ Kafka connected");
}

async function setContainer(containerId, containerName = containerId) {
  await redis.hset(`container:${containerId}`, {
    containerId,
    containerName,
    lastSeen: Date.now(),
  });
}

async function deleteContainer(containerId) {
  await redis.del(`container:${containerId}`);
}

async function getAllContainers() {
  const keys = await redis.keys("container:*");
  const containers = [];
  for (const key of keys) {
    const data = await redis.hgetall(key);
    if (data) containers.push(data);
  }
  return containers;
}

async function setupMinio() {
  const bucketExists = await minioClient.bucketExists("logs");
  if (!bucketExists) {
    await minioClient.makeBucket("logs");
    console.log("✅ MinIO logs bucket created");
  } else {
    console.log("✅ MinIO connected");
  }
}

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

    process.on("close", async () => {
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

        const inspect = spawn("docker", [
          "inspect",
          "--format",
          "{{.Name}}",
          containerId,
        ]);

        let containerName = "";
        inspect.stdout.on("data", (data) => {
          containerName += data.toString().trim().replace("/", "");
        });

        inspect.on("close", async () => {
          await setContainer(containerId, containerName);
          await prisma.container.create({
            data: {
              id: containerId,
              name: containerName,
            },
          });

          const worker = spawn("node", ["worker.js", containerId, "8080"], {
            detached: false,
            stdio: "inherit",
          });

          worker.on("error", (err) =>
            console.error("Worker failed to start:", err),
          );

          return res
            .status(200)
            .json({ message: "container started", containerId });
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

    process.on("close", async () => {
      await deleteContainer(containerId);

      return res.status(200).json({ message: "container stopped" });
    });
  } catch (error) {
    return res.status(500).json({
      message: "error",
    });
  }
});

// Function to execute commands in the container
app.post("/exec", async (req, res) => {
  const { command, containerId } = req.body;
  const commandId = `cmd-${Date.now()}-${Math.random().toString(36).slice(2)}`;

  await producer.send({
    topic: "commands",
    messages: [
      {
        value: JSON.stringify({
          commandId,
          command,
          targetContainerId: containerId,
        }),
      },
    ],
  });

  const output = await new Promise((resolve, reject) => {
    pendingResults.set(commandId, resolve);
    setTimeout(() => {
      pendingResults.delete(commandId);
      reject(new Error("timeout"));
    }, 10000);
  });

  res.json({ output });
});

// Function to get the list of containers
app.get("/listContainers", async (req, res) => {
  try {
    const containers = await prisma.container.findMany();
    const containerList = containers.map((c) => c.name);
    const containerListId = containers.map((c) => c.id);
    return res
      .status(200)
      .json({ message: "success", containerList, containerListId });
  } catch (error) {
    return res.status(500).json({ message: "error" });
  }
});

app.post("/startContainer", (req, res) => {
  const { spawn } = require("child_process");

  const containerId = req.body.selectedContainer;

  try {
    const process = spawn("docker", ["start", containerId]);

    process.on("close", async () => {
      const inspect = spawn("docker", [
        "inspect",
        "--format",
        "{{.Name}}",
        containerId,
      ]);
      let containerName = "";

      inspect.stdout.on("data", (data) => {
        containerName += data.toString().trim().replace("/", "");
      });

      inspect.on("close", async () => {
        await setContainer(containerId, containerName);

        const worker = spawn("node", ["worker.js", containerId, "8080"], {
          detached: false,
          stdio: "inherit",
        });

        worker.on("error", (err) =>
          console.error("Worker failed to start:", err),
        );

        return res.status(200).json({ message: "container started" });
      });
    });
  } catch (error) {
    return res.status(500).json({
      message: "error",
    });
  }
});

app.post("/heartbeat", express.json(), async (req, res) => {
  const { containerId } = req.body;

  const exists = await redis.exists(`container:${containerId}`);

  if (exists) {
    await redis.hset(`container:${containerId}`, "lastSeen", Date.now());
  }

  res.sendStatus(200);
});

setInterval(async () => {
  const now = Date.now();
  const allContainers = await getAllContainers();

  if (allContainers.length === 0) return;

  for (const c of allContainers) {
    if (!c.lastSeen || isNaN(parseInt(c.lastSeen))) continue;
    if (now - parseInt(c.lastSeen) > 20000) {
      const { spawn } = require("child_process");
      console.log("Auto-stopping:", c.containerId);

      try {
        const process = spawn("docker", ["stop", c.containerId]);
        process.on("exit", async () => {
          await deleteContainer(c.containerId);
        });
      } catch (err) {
        console.error(err);
      }
    }
  }
}, 5000);

startKafka();
setupMinio();

app.listen(port, () => {
  console.log(`Example app listening on port ${port}`);
});
