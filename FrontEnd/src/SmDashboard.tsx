import { useEffect, useState } from "react";
import ThemeToggle from "./ThemeToggle";

function SmDashboard() {
  const [sprints, setSprints] = useState([]);
  const [view, setView] = useState("sprints");
  const [sprintForm, setSprintForm] = useState({
    name: "",
    goal: "",
    endDate: "",
  });
  const [taskForm, setTaskForm] = useState({
    name: "",
    description: "",
    priority: "MEDIUM",
    userId: "",
  });
  const [selectedSprint, setSelectedSprint] = useState<any>(null);
  const [team, setTeam] = useState([]);
  const [users, setUsers] = useState([]);
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const [memberForm, setMemberForm] = useState({ userId: "" });

  const addToTeam = () => {
    fetch("http://localhost:8080/api/addToTeam", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userId: memberForm.userId,
        scrumMasterId: user.id,
      }),
    }).then(() => setView("sprints"));
  };

  const fetchSprints = () =>
    fetch(`http://localhost:8080/api/sprints/${user.id}`)
      .then((r) => r.json())
      .then((d) => setSprints(d.sprints || []));

  const fetchTeam = () =>
    fetch(`http://localhost:8080/api/team/${user.id}`)
      .then((r) => r.json())
      .then((d) => setTeam(d.team || []));

  const fetchUsers = () =>
    fetch(`http://localhost:8080/api/users`)
      .then((r) => r.json())
      .then((d) => setUsers(d.users || []));

  useEffect(() => {
    fetchSprints();
  }, []);

  const createSprint = () => {
    fetch("http://localhost:8080/api/createSprint", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        ...sprintForm,
        scrumMaster_Id: user.id,
        endDate: `${sprintForm.endDate}T00:00:00`,
      }),
    }).then(() => {
      fetchSprints();
      setView("sprints");
    });
  };

  const addTask = () => {
    fetch("http://localhost:8080/api/addTask", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        ...taskForm,
        sprintId: selectedSprint.id,
        scrumMasterId: user.id,
      }),
    }).then(() => {
      fetchSprints();
      setView("sprints");
    });
  };

  const startSprint = (sprintId: string) =>
    fetch(`http://localhost:8080/api/startSprint/${sprintId}`, {
      method: "PUT",
    }).then(() => fetchSprints());

  const completeSprint = (sprintId: string) =>
    fetch(`http://localhost:8080/api/completeSprint/${sprintId}`, {
      method: "PUT",
    }).then(() => fetchSprints());

  return (
    <div className="relative flex flex-col min-h-screen w-screen p-6 overflow-hidden bg-(--color-bg-main) text-(--color-text-main)">
      <div className="absolute inset-0 pointer-events-none opacity-60 grid-texture" />
      <div className="absolute pointer-events-none rounded-full cyan-orb" />

      {/* Header */}
      <div className="relative z-10 flex items-center justify-between w-full max-w-5xl mx-auto mb-6">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-(--color-primary-light) border border-(--color-border-accent) flex items-center justify-center">
            <span className="text-sm text-(--color-primary)">⬡</span>
          </div>
          <div>
            <p className="text-sm font-semibold m-0 tracking-tight">
              SM Dashboard
            </p>
            <p className="font-mono text-[11px] text-(--color-text-muted) m-0">
              {user.name}
            </p>
          </div>
        </div>
        <ThemeToggle />
      </div>

      {/* Sprints view */}
      {view === "sprints" && (
        <div className="relative z-10 max-w-5xl mx-auto w-full flex flex-col gap-4">
          <div className="flex gap-2">
            <button
              onClick={() => {
                fetchUsers();
                setView("addToTeam");
              }}
              className="font-mono px-4 py-2 text-[12px] font-semibold bg-(--color-bg-card) text-(--color-text-muted) border border-(--color-border) rounded-lg transition active:scale-[0.985] hover:opacity-90"
            >
              + add member
            </button>
            <button
              onClick={() => setView("createSprint")}
              className="font-mono px-4 py-2 text-[12px] font-semibold bg-(--color-primary) text-(--color-button-text) rounded-lg transition active:scale-[0.985] hover:opacity-90"
            >
              + new sprint
            </button>
          </div>

          {sprints.length === 0 && (
            <div className="bg-(--color-bg-card) border border-(--color-border) rounded-2xl p-8 text-center font-mono text-[13px] text-(--color-text-muted)">
              no sprints yet
            </div>
          )}

          {sprints.map((sprint: any) => (
            <div
              key={sprint.id}
              className="bg-(--color-bg-card) border border-(--color-border-strong) rounded-2xl p-6 flex flex-col gap-3"
            >
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-semibold text-[15px] m-0">{sprint.name}</p>
                  <p className="font-mono text-[11px] text-(--color-text-muted) m-0 mt-1">
                    {sprint.goal}
                  </p>
                </div>
                <span className="font-mono text-[11px] px-2 py-1 rounded-md border border-(--color-border) text-(--color-text-muted)">
                  {sprint.status}
                </span>
              </div>

              <div className="flex gap-6 font-mono text-[11px] text-(--color-text-muted)">
                <span>
                  start:{" "}
                  <span className="text-(--color-text-main)">
                    {sprint.startDate ?? "—"}
                  </span>
                </span>
                <span>
                  end:{" "}
                  <span className="text-(--color-text-main)">
                    {sprint.endDate}
                  </span>
                </span>
              </div>

              {sprint.status === "PLANNING" && (
                <button
                  onClick={() => startSprint(sprint.id)}
                  className="font-mono w-full py-2 text-[12px] font-semibold bg-(--color-primary-light) text-(--color-primary) border border-(--color-border-accent) rounded-lg transition hover:bg-(--color-primary-glow) active:scale-[0.98]"
                >
                  start sprint →
                </button>
              )}

              {sprint.status === "ACTIVE" && (
                <div className="flex gap-2">
                  <button
                    onClick={() => {
                      setSelectedSprint(sprint);
                      fetchTeam();
                      setView("addTask");
                    }}
                    className="font-mono flex-1 py-2 text-[12px] font-semibold bg-(--color-bg-muted) text-(--color-text-main) border border-(--color-border) rounded-lg transition hover:border-(--color-border-accent) active:scale-[0.98]"
                  >
                    + task
                  </button>
                  <button
                    onClick={() => completeSprint(sprint.id)}
                    className="font-mono flex-1 py-2 text-[12px] font-semibold bg-(--color-success-bg) text-(--color-success) border border-(--color-border) rounded-lg transition hover:opacity-90 active:scale-[0.98]"
                  >
                    complete ✓
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Create sprint view */}
      {view === "createSprint" && (
        <div className="relative z-10 max-w-sm mx-auto w-full bg-(--color-bg-card) border border-(--color-border-strong) rounded-2xl p-7 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <p className="font-mono text-[10px] text-(--color-text-muted) uppercase tracking-widest m-0">
              New Sprint
            </p>
            <button
              onClick={() => setView("sprints")}
              className="font-mono text-[11px] text-(--color-text-muted) hover:text-(--color-text-main) transition"
            >
              ← back
            </button>
          </div>

          <div className="h-px bg-(--color-border) -mx-7" />

          {[
            { placeholder: "sprint name", key: "name", icon: "◎" },
            { placeholder: "sprint goal", key: "goal", icon: "◈" },
          ].map(({ placeholder, key, icon }) => (
            <div key={key} className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[13px] text-(--color-primary) pointer-events-none">
                {icon}
              </span>
              <input
                type="text"
                placeholder={placeholder}
                onChange={(e) =>
                  setSprintForm((f) => ({ ...f, [key]: e.target.value }))
                }
                className="font-mono w-full pl-9 pr-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) focus:border-(--color-border-accent) transition"
              />
            </div>
          ))}

          <div className="flex flex-col gap-1">
            <p className="font-mono text-[10px] text-(--color-text-muted) uppercase tracking-widest m-0">
              end date
            </p>
            <input
              type="date"
              onChange={(e) =>
                setSprintForm((f) => ({ ...f, endDate: e.target.value }))
              }
              className="font-mono w-full px-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) focus:border-(--color-border-accent) transition"
            />
          </div>

          <button
            onClick={createSprint}
            className="font-mono w-full py-2.5 bg-(--color-primary) text-(--color-button-text) rounded-lg text-[13px] font-semibold tracking-wider transition active:scale-[0.985] hover:opacity-90"
          >
            create sprint →
          </button>
        </div>
      )}

      {/* Add task view */}
      {view === "addTask" && (
        <div className="relative z-10 max-w-sm mx-auto w-full bg-(--color-bg-card) border border-(--color-border-strong) rounded-2xl p-7 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <p className="font-mono text-[10px] text-(--color-text-muted) uppercase tracking-widest m-0">
              Add Task — {selectedSprint?.name}
            </p>
            <button
              onClick={() => setView("sprints")}
              className="font-mono text-[11px] text-(--color-text-muted) hover:text-(--color-text-main) transition"
            >
              ← back
            </button>
          </div>

          <div className="h-px bg-(--color-border) -mx-7" />

          {[
            { placeholder: "task name", key: "name", icon: "◎" },
            { placeholder: "description", key: "description", icon: "◈" },
          ].map(({ placeholder, key, icon }) => (
            <div key={key} className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[13px] text-(--color-primary) pointer-events-none">
                {icon}
              </span>
              <input
                type="text"
                placeholder={placeholder}
                onChange={(e) =>
                  setTaskForm((f) => ({ ...f, [key]: e.target.value }))
                }
                className="font-mono w-full pl-9 pr-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) placeholder-(--color-text-muted) focus:outline-none focus:ring-2 focus:ring-(--color-focus) focus:border-(--color-border-accent) transition"
              />
            </div>
          ))}

          <div className="flex flex-col gap-1">
            <p className="font-mono text-[10px] text-(--color-text-muted) uppercase tracking-widest m-0">
              assign to
            </p>
            <select
              value={taskForm.userId}
              onChange={(e) =>
                setTaskForm((f) => ({ ...f, userId: e.target.value }))
              }
              className="font-mono w-full px-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
            >
              <option value="">unassigned</option>
              {team.map((u: any) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1">
            <p className="font-mono text-[10px] text-(--color-text-muted) uppercase tracking-widest m-0">
              priority
            </p>
            <div className="flex rounded-lg overflow-hidden border border-(--color-border) font-mono text-[12px]">
              {["LOW", "MEDIUM", "HIGH", "CRITICAL"].map((p) => (
                <button
                  key={p}
                  onClick={() => setTaskForm((f) => ({ ...f, priority: p }))}
                  className={`flex-1 py-2 transition ${
                    taskForm.priority === p
                      ? "bg-(--color-primary) text-(--color-button-text) font-semibold"
                      : "bg-(--color-bg-muted) text-(--color-text-muted)"
                  }`}
                >
                  {p.toLowerCase()}
                </button>
              ))}
            </div>
          </div>

          <button
            onClick={addTask}
            className="font-mono w-full py-2.5 bg-(--color-primary) text-(--color-button-text) rounded-lg text-[13px] font-semibold tracking-wider transition active:scale-[0.985] hover:opacity-90"
          >
            add task →
          </button>
        </div>
      )}

      {/* Add to team view */}
      {view === "addToTeam" && (
        <div className="relative z-10 max-w-sm mx-auto w-full bg-(--color-bg-card) border border-(--color-border-strong) rounded-2xl p-7 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <p className="font-mono text-[10px] text-(--color-text-muted) uppercase tracking-widest m-0">
              Add Team Member
            </p>
            <button
              onClick={() => setView("sprints")}
              className="font-mono text-[11px] text-(--color-text-muted) hover:text-(--color-text-main) transition"
            >
              ← back
            </button>
          </div>

          <div className="h-px bg-(--color-border) -mx-7" />

          <div className="flex flex-col gap-1">
            <p className="font-mono text-[10px] text-(--color-text-muted) uppercase tracking-widest m-0">
              select user
            </p>
            <select
              value={memberForm.userId}
              onChange={(e) =>
                setMemberForm((f) => ({ ...f, userId: e.target.value }))
              }
              className="font-mono w-full px-3 py-2.5 text-[13px] bg-(--color-bg-muted) border border-(--color-border) rounded-lg text-(--color-text-main) focus:outline-none focus:ring-2 focus:ring-(--color-focus) transition"
            >
              <option value="">select user</option>
              {users.map((u: any) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </select>
          </div>

          <button
            onClick={addToTeam}
            className="font-mono w-full py-2.5 bg-(--color-primary) text-(--color-button-text) rounded-lg text-[13px] font-semibold tracking-wider transition active:scale-[0.985] hover:opacity-90"
          >
            add member →
          </button>
        </div>
      )}
    </div>
  );
}

export default SmDashboard;
