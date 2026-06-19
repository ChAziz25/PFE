import { useEffect, useState } from "react";

function TaskPanel() {
  const [tasks, setTasks] = useState([]);
  const user = JSON.parse(localStorage.getItem("user") || "{}");

  useEffect(() => {
    fetch(`http://localhost:8080/api/myTasks/${user.id}`)
      .then((r) => r.json())
      .then((d) => setTasks(d.tasks || []));
  }, []);

  const updateStatus = (taskId: string, status: string) => {
    fetch(`http://localhost:8080/api/updateStatus/${taskId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status }),
    }).then(() =>
      fetch(`http://localhost:8080/api/myTasks/${user.id}`)
        .then((r) => r.json())
        .then((d) => setTasks(d.tasks || [])),
    );
  };

  const nextStatus: Record<string, string> = {
    TODO: "IN_PROGRESS",
    IN_PROGRESS: "IN_REVIEW",
    IN_REVIEW: "DONE",
  };

  if (tasks.length === 0) return null;

  return (
    <div className="relative z-10 w-full max-w-4xl mx-auto mt-6 flex flex-col gap-3">
      <p className="font-mono text-[10px] text-(--color-text-muted) uppercase tracking-widest m-0">
        my tasks
      </p>
      {tasks.map((t: any) => (
        <div
          key={t.id}
          className="bg-(--color-bg-card) border border-(--color-border-strong) rounded-xl px-5 py-3 flex items-center justify-between gap-4"
        >
          <div className="flex flex-col gap-1">
            <p className="font-mono text-[13px] m-0">{t.name}</p>
            <span className="font-mono text-[10px] text-(--color-text-muted)">
              {t.priority}
            </span>
          </div>
          <div className="flex items-center gap-3">
            <span className="font-mono text-[11px] text-(--color-text-muted)">
              {t.status}
            </span>
            {t.status !== "DONE" && (
              <button
                onClick={() => updateStatus(t.id, nextStatus[t.status])}
                className="font-mono px-3 py-1.5 text-[11px] font-semibold bg-(--color-primary-light) text-(--color-primary) border border-(--color-border-accent) rounded-lg transition hover:bg-(--color-primary-glow) active:scale-[0.98]"
              >
                → {nextStatus[t.status]?.toLowerCase().replace("_", " ")}
              </button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}

export default TaskPanel;
