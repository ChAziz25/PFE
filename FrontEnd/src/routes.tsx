import { lazy, Suspense } from "react";
import { Routes, Route } from "react-router-dom";
import useTheme from "./hooks/useTheme";
import LoadingScreen from "./components/LoadingScreen";

const App = lazy(() => import("./App"));
const Login = lazy(() => import("./Login"));
const SignUp = lazy(() => import("./SignUp"));
const Profile = lazy(() => import("./Profile"));

function Routes_() {
  useTheme();

  return (
    <Suspense fallback={<LoadingScreen />}>
      <Routes>
        <Route path="/" element={<App />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<SignUp />} />
        <Route path="/profile" element={<Profile />} />
      </Routes>
    </Suspense>
  );
}

export default Routes_;
