import { Routes, Route } from "react-router-dom";
import App from "./App";
import Settings from "./Settings";

function routes() {
  return (
    <Routes>
      <Route path="/" element={<App />} />
      <Route path="/Settings" element={<Settings />} />
    </Routes>
  );
}

export default routes;
