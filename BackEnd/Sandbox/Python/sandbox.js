const { spawn } = require("child_process");

function PythonSandbox(text, req, res) {
  const Sandbox = spawn("docker", [
    "run",
    "--rm",
    "python-sandbox:latest",
    "python3",
    "-c",
    text,
  ]);
  let output = "";

  Sandbox.stdout.on("data", (data) => {
    output += data.toString();
  });

  Sandbox.stderr.on("data", (data) => {
    output += data.toString();
  });

  Sandbox.on("close", (code) => {
    res.send(output);
  });
}

module.exports = PythonSandbox;
