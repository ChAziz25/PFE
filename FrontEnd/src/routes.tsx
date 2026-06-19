import { lazy, Suspense } from "react";
import { Routes, Route } from "react-router-dom";
import useTheme from "./hooks/useTheme";
import LoadingScreen from "./components/LoadingScreen";

const App = lazy(() => import("./App"));
const Login = lazy(() => import("./Login"));
const SignUp = lazy(() => import("./SignUp"));
const Profile = lazy(() => import("./Profile"));
const SmDashboard = lazy(() => import("./SmDashboard"));

function Routes_() {
  useTheme();

  return (
    <Suspense fallback={<LoadingScreen />}>
      <Routes>
        <Route path="/" element={<App />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<SignUp />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/sm-dashboard" element={<SmDashboard />} />
      </Routes>
    </Suspense>
  );
}

export default Routes_;
