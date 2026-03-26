import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import settings from "./assets/gear-svgrepo-com.svg";
import sun from "./assets/icons8-sun.svg";
import moon from "./assets/moon-svgrepo-com.svg";

function App() {
  const [options, setOptions] = useState([]);
  const [lang, setLang] = useState("");
  const [text, setText] = useState("");
  const [response, setResponse] = useState("");

  const [theme, setTheme] = useState("dark");

  useEffect(() => {
    async function loadLanguages() {
      try {
        const res = await fetch("http://localhost:3000/languages");
        const data = await res.json();
        setOptions(data);
        setLang(data[0]);
      } catch (err) {
        console.error(err);
      }
    }

    loadLanguages();
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  function HandleSubmit() {
    fetch("http://localhost:3000/run", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        text: text,
        lang: lang,
      }),
    })
      .then((res) => res.json())
      .then((data) => setResponse(data.result))
      .catch((err) => console.log(err));
  }

  return (
    <>
      <div className="absolute top-4 right-4 justify-between">
        <Link to="/Settings">
          <button className="mr-2 ml-2">
            <img src={settings} width="28" height="28" alt="settings" />
          </button>
        </Link>
        {theme === "dark" ? (
          <button className="mr-2 ml-2" onClick={() => setTheme("light")}>
            <img src={moon} width="28" height="28" alt="light" />
          </button>
        ) : (
          <button className="mr-2 ml-2" onClick={() => setTheme("dark")}>
            <img src={sun} width="28" height="28" alt="dark" />
          </button>
        )}
      </div>
      <div className="min-h-screen bg-[var(--color-bg-main)] flex items-center justify-center p-6">
        <div className="w-full max-w-xl bg-[var(--color-bg-card)] shadow-lg rounded-xl p-8 space-y-6">
          <h1 className="text-3xl font-bold text-center text-[var(--color-text-main)]">
            FrontEnd
          </h1>

          <div className="space-y-2">
            <h2 className="text-lg font-semibold text-[var(--color-text-secondary)]">
              Input
            </h2>

            <textarea
              onChange={(e) => setText(e.target.value)}
              placeholder="Type your text here..."
              className="w-full h-32 p-3 border border-[var(--color-border-strong)] rounded-lg resize-none
        bg-[var(--color-bg-card)]
        focus:outline-none focus:ring-2 focus:ring-[var(--color-focus)] text-[var(--color-text-main)]"
            />

            {options.length > 0 && (
              <select
                value={lang}
                onChange={(e) => setLang(e.target.value)}
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
          </div>

          <button
            onClick={HandleSubmit}
            className="w-full bg-[var(--color-primary)] text-white py-3 rounded-lg font-semibold
      hover:bg-[var(--color-primary-hover)] transition"
          >
            Submit
          </button>

          <div className="space-y-2">
            <h2 className="text-lg font-semibold text-[var(--color-text-secondary)]">
              Output
            </h2>

            <div className="min-h-[80px] max-h-60 overflow-y-auto p-4 border border-[var(--color-border)] rounded-lg bg-[var(--color-bg-muted)]">
              <p className="text-[var(--color-text-main)] whitespace-pre-wrap">
                {response}
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

export default App;
