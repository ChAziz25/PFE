package com.PFE.backend.controllers;

import com.PFE.backend.models.Task;
import com.PFE.backend.models.TaskStatus;
import com.PFE.backend.repositories.TaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @PutMapping("/updateStatus/{taskId}")
    public ResponseEntity<?> updateStatus(@PathVariable String taskId, @RequestBody Map<String, String> body) {
        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            task.setStatus(TaskStatus.valueOf(body.get("status")));
            taskRepository.save(task);

            return ResponseEntity.ok(Map.of("message", "Status updated", "status", task.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/myTasks/{userId}")
    public ResponseEntity<?> myTasks(@PathVariable String userId) {
        List<Task> tasks = taskRepository.findByTaskFor_Id(userId);
        return ResponseEntity.ok(Map.of("tasks", tasks.stream().map(t -> Map.of(
                "id", t.getId(),
                "name", t.getName(),
                "status", t.getStatus(),
                "priority", t.getPriority()
        )).toList()));
    }

    @GetMapping("/sprintTasks/{sprintId}")
    public ResponseEntity<?> sprintTasks(@PathVariable String sprintId) {
        List<Task> tasks = taskRepository.findBySprint_Id(sprintId);
        return ResponseEntity.ok(Map.of("tasks", tasks.stream().map(t -> Map.of(
                "id", t.getId(),
                "name", t.getName(),
                "status", t.getStatus(),
                "priority", t.getPriority(),
                "assignedTo", t.getTaskFor() != null ? t.getTaskFor().getName() : "unassigned"
        )).toList()));
    }
}
