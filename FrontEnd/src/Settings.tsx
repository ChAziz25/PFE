import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import sun from "./assets/icons8-sun.svg";
import moon from "./assets/moon-svgrepo-com.svg";

function Settings() {
  const [theme, setTheme] = useState("dark");
  const [lang, setLang] = useState("");
  const [options, setOptions] = useState([]);
  const [response, setResponse] = useState([]);
  const [ResImports, setResImports] = useState<string[]>([]);
  const [ResImportsFrom, setResImportsFrom] = useState<string[]>([]);
  const [ResArgs, setResArgs] = useState<string[]>([]);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  useEffect(() => {
    async function loadLanguages() {
      try {
        const res = await fetch("http://localhost:3000/languages");
        const data = await res.json();
        setOptions(data);
        setLang(data[0]);
        ShowSettings(data[0]);
      } catch (err) {
        console.error(err);
      }
    }

    loadLanguages();
  }, []);

  function ShowSettings(lang: string) {
    fetch("http://localhost:3000/settings", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        lang: lang,
      }),
    })
      .then((res) => res.json())
      .then((data) => {
        setResponse(data);
        HandleResponse(data);
      })
      .catch((err) => console.log(err));
  }

  function HandleResponse(lines: Array<string>) {
    setResImports([]);
    setResImportsFrom([]);
    setResArgs([]);

    lines.forEach((line) => {
      let words = line.trim().split(" ");
      if (words[0] === "import") {
        setResImports((prevResImports) => [...prevResImports, words[1]]);
      } else if (words[0] === "from") {
        setResImportsFrom((prevResImportsFrom) => [
          ...prevResImportsFrom,
          words[3],
        ]);
        setResImportsFrom((prevResImportsFrom) => [
          ...prevResImportsFrom,
          words[1],
        ]);
      } else {
        const rest = words.slice(2).join(" ");
        const matches = rest.match(/"([^"]*)"/g);

        if (matches) {
          const cleaned = matches.map((m) => m.replace(/"/g, "")).join(" ");
          setResArgs((prevResArgs) => [...prevResArgs, cleaned]);
        } else {
          setResArgs((prevResArgs) => [...prevResArgs, rest]);
        }
      }
    });
  }

  return (
    <>
      <div className="absolute top-3 left-3 right-3 width-full flex justify-between">
        <Link to="/">
          <button className="flex items-center space-x-2 bg-[var(--color-primary)] hover:bg-[var(--color-primary-hover)] text-[var(--color-text-main)] font-medium py-2 px-4 rounded-lg shadow-sm transition-colors duration-200">
            {/* Arrow Icon */}
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15 19l-7-7 7-7"
              ></path>
            </svg>
            <span>Back</span>
          </button>
        </Link>
        {theme === "dark" ? (
          <button onClick={() => setTheme("light")}>
            <img src={moon} width="28" height="28" alt="Moon" />
          </button>
        ) : (
          <button onClick={() => setTheme("dark")}>
            <img src={sun} width="28" height="28" alt="Sun" />
          </button>
        )}
      </div>
      <div className="min-h-screen bg-[var(--color-bg-main)] flex items-center justify-center p-6">
        <div className="max-w-md w-full space-y-8">
          <div className="min-h-screen bg-[var(--color-bg-main)] flex items-center justify-center p-6">
            <div className="w-full max-w-xl bg-[var(--color-bg-card)] shadow-lg rounded-xl p-8 space-y-6">
              <h1 className="text-3xl font-bold text-center text-[var(--color-text-main)]">
                Settings
              </h1>
              {options.length > 0 && (
                <select
                  value={lang}
                  onChange={(e) => {
                    const newLang = e.target.value;
                    setLang(newLang);
                    ShowSettings(newLang);
                  }}
                  className="w-full p-3 border border-[var(--color-border-strong)] rounded-lg
        bg-[var(--color-bg-card)]
        focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)] text-[var(--color-text-main)]"
                >
                  {options.map((option, index) => (
                    <option key={index} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              )}

              <div className="p-4 border border-[var(--color-border)] rounded-lg bg-[var(--color-bg-muted)] text-[var(--color-text-main)]">
                {response ? (
                  <>
                    <div className="flex">
                      {ResImports.length > 0 && (
                        <>
                          <p className="p-2">Imports: </p>
                          <input
                            type="text"
                            className="bg-[var(--color-bg-card)] p-2 rounded-lg border-[var(--color-border)]"
                            value={ResImports.join(", ")}
                            onChange={console.log}
                          />
                        </>
                      )}
                    </div>
                    <div className="flex">
                      {ResImportsFrom.length > 0 && (
                        <>
                          <p className="p-2">Imports: </p>
                          <input
                            type="text"
                            className="bg-[var(--color-bg-card)] p-2 rounded-lg border-[var(--color-border)]"
                            value={ResImportsFrom.join(", ")}
                            onChange={console.log}
                          />
                        </>
                      )}
                    </div>
                    <div className="flex">
                      {ResArgs.length > 0 && (
                        <>
                          <p className="p-2">Arguments: </p>
                          <input
                            type="text"
                            className="bg-[var(--color-bg-card)] p-2 rounded-lg border-[var(--color-border)]"
                            value={ResArgs.join(", ")}
                            onChange={console.log}
                          />
                        </>
                      )}
                    </div>
                  </>
                ) : (
                  <p>Waiting ...</p>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

export default Settings;
