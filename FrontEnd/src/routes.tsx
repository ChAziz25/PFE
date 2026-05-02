import { Routes, Route } from "react-router-dom";
import App from "./App";
import Login from "./Login";
import SignUp from "./SignUp";
import Profile from "./Profile";

function routes() {
  return (
    <Routes>
      <Route path="/" element={<App />} />
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<SignUp />} />
      <Route path="/profile" element={<Profile />} />
    </Routes>
  );
}

export default routes;
