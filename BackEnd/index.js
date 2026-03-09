const express = require("express");
const cors = require("cors");
const fs = require("fs").promises;
const path = require("path");

const PythonSandbox = require("./Sandbox/Python/sandbox");

const app = express();
const port = 3000;

app.use(cors());
app.use(express.json());

app.get("/", async (req, res) => {
  const dirPath = path.join(__dirname, "Sandbox");

  try {
    await fs.access(dirPath);

    const files = await fs.readdir(dirPath);
    const directories = [];

    for (const file of files) {
      const fullPath = path.join(dirPath, file);
      const stat = await fs.lstat(fullPath);

      if (stat.isDirectory()) {
        directories.push(file);
      }
    }

    res.json(directories);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Failed to fetch directories" });
  }
});

app.post("/", (req, res) => {
  const text = req.body.text;
  const lang = req.body.lang;

  switch (lang) {
    case "Python":
      PythonSandbox(text, req, res);
      break;
    default:
      return res.status(400).json({ error: "Invalid language" });
  }
});

app.listen(port, () => {
  console.log(`Example app listening on port http://localhost:${port}`);
});
