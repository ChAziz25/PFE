const { spawn } = require("child_process");
const fs = require("fs").promises;
const path = require("path");

async function deleteFolder(localTmpPath) {
  try {
    await fs.rm(`${localTmpPath}`, {
      recursive: true,
      force: false,
    });
    console.log("tmp folder deleted successfully");
  } catch (err) {
    console.error("Error deleting folder:", err);
  }
}

async function JavaSandbox(text, req, res) {
  const localTmpPath = path.join(process.cwd(), "Sandbox", "Java", "tmp");
  const tempFilePath = "Main.java";

  const writeFileCommand = spawn("docker", [
    "run",
    "--rm",
    "-i",
    "-v",
    `${localTmpPath}:/tmp`,
    "java-sandbox:latest",
    "bash",
    "-c",
    `echo "${text.replace(/"/g, '\\"')}" > /tmp/${tempFilePath}`,
  ]);

  writeFileCommand.on("close", async (code) => {
    if (code !== 0) {
      return res
        .status(500)
        .send({ error: "Failed to create Java file in container." });
    }

    const compile = spawn("docker", [
      "run",
      "--rm",
      "-v",
      `${localTmpPath}:/tmp`,
      "java-sandbox:latest",
      "bash",
      "-c",
      "javac /tmp/Main.java",
    ]);

    let output = "";

    compile.stdout.on("data", (data) => {
      output += data.toString();
    });

    compile.stderr.on("data", (data) => {
      output += data.toString();
    });

    compile.on("close", async (code) => {
      if (code !== 0) {
        return res.status(500).send(`Compilation failed: ${output}`);
      }

      const run = spawn("docker", [
        "run",
        "--rm",
        "-v",
        `${localTmpPath}:/tmp`,
        "java-sandbox:latest",
        "bash",
        "-c",
        "java -cp /tmp Main",
      ]);

      let runtimeOutput = "";

      run.stdout.on("data", (data) => {
        runtimeOutput += data.toString();
      });

      run.stderr.on("data", (data) => {
        runtimeOutput += data.toString();
      });

      run.on("close", (code) => {
        res.send(runtimeOutput);
        deleteFolder(localTmpPath);
      });
    });
  });
}

module.exports = JavaSandbox;
