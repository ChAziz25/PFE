import { useState } from "react";
import { Link } from "react-router-dom";

export default function Auth() {
  const [user] = useState(() => {
    const stored = localStorage.getItem("user");
    return stored ? JSON.parse(stored) : null;
  });

  const handleLogout = () => {
    localStorage.removeItem("user");
    window.location.href = "/login";
  };

  return (
    <div className="fixed top-5 right-5">
      {user ? (
        <div className="flex items-center gap-3">
          <Link to="/profile">
          <span className="text-sm">{user.name}</span>
          </Link>
          <button
            onClick={handleLogout}
            className="bg-red-500 hover:bg-red-600 text-white rounded-full px-4 py-2 text-sm transition"
          >
            logout
          </button>
        </div>
      ) : (
        <Link
          to="/login"
          className="bg-blue-500 hover:bg-blue-600 text-white rounded-full px-4 py-2 text-sm transition"
        >
          sign in
        </Link>
      )}
    </div>
  );
}
