const { Kafka } = require("kafkajs");
const Minio = require("minio");

const containerId = process.argv[2];
const port = process.argv[3];

const minioClient = new Minio.Client({
  endPoint: "localhost",
  port: 9000,
  useSSL: false,
  accessKey: "sandbox",
  secretKey: "sandbox123",
});

if (!containerId || !port) {
  console.error("Usage: node worker.js <containerId> <port>");
  process.exit(1);
}

const kafka = new Kafka({
  clientId: `worker-${containerId}`,
  brokers: ["localhost:9092"],
});

const consumer = kafka.consumer({ groupId: `worker-${containerId}` });
const producer = kafka.producer();

async function start() {
  await consumer.connect();
  await producer.connect();
  await consumer.subscribe({ topic: "commands", fromBeginning: false });

  console.log(`✅ Worker started for container ${containerId}`);

  await consumer.run({
    eachMessage: async ({ message }) => {
      const { commandId, command, targetContainerId } = JSON.parse(
        message.value.toString(),
      );

      if (targetContainerId !== containerId) return;

      try {
        const response = await fetch(`http://127.0.0.1:${port}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ command }),
        });

        const data = await response.json();

        await producer.send({
          topic: "results",
          messages: [
            { value: JSON.stringify({ commandId, output: data.output }) },
          ],
        });

        const timestamp = new Date().toISOString();
        const logKey = `${containerId}/${timestamp}.txt`;
        const logContent = `COMMAND:\n${command}\n\nOUTPUT:\n${data.output}`;

        await minioClient.putObject(
          "logs",
          logKey,
          Buffer.from(logContent),
          logContent.length,
          { "Content-Type": "text/plain" },
        );
      } catch (err) {
        await producer.send({
          topic: "results",
          messages: [
            {
              value: JSON.stringify({
                commandId,
                output: `Error: ${err.message}`,
              }),
            },
          ],
        });
      }
    },
  });
}

start();

process.on("SIGTERM", async () => {
  await consumer.disconnect();
  await producer.disconnect();
  process.exit(0);
});
