import { useEffect, useState } from "react";

function App() {
  const [options, setOptions] = useState([]);
  const [lang, setLang] = useState("");
  const [text, setText] = useState("");
  const [response, setResponse] = useState("");

  useEffect(() => {
    fetch("http://localhost:3000/")
      .then((res) => {
        if (!res.ok) throw new Error(res.statusText);
        return res.json();
      })
      .then((data) => {
        setOptions(data);
        setLang(data[0]);
      })
      .catch((err) => console.log(err));
  }, []);

  function HandleSubmit() {
    fetch("http://localhost:3000/", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ text: "print('hello world')", lang: lang }),
    })
      .then((res) => res.text())
      .then((data) => setResponse(data))
      .catch((err) => console.log(err));
  }

  return (
    <>
      <h1>FrontEnd</h1>
      <h2>input</h2>
      <textarea onChange={(e) => setText(e.target.value)}></textarea>
      <select onChange={(e) => setLang(e.target.value)}>
        {options.map((option, index) => (
          <option key={index} value={option}>
            {option}
          </option>
        ))}
      </select>
      <br />
      <button onClick={HandleSubmit}>Submit</button>
      <h2>output</h2>
      <p>{response}</p>
    </>
  );
}

export default App;
