package com.PFE.backend.controllers;

import com.PFE.backend.models.*;
import com.PFE.backend.repositories.ScrumMasterRepository;
import com.PFE.backend.repositories.SprintRepository;
import com.PFE.backend.repositories.TaskRepository;
import com.PFE.backend.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class ScrumMasterController {
    private final ScrumMasterRepository scrumMasterRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public ScrumMasterController(ScrumMasterRepository scrumMasterRepository, SprintRepository sprintRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.scrumMasterRepository = scrumMasterRepository;
        this.sprintRepository = sprintRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @PostMapping("/createSM")
    public ResponseEntity<?> createSM(@RequestBody Map<String, String> body) {
        ScrumMaster sm = new ScrumMaster();
        sm.setName(body.get("name"));
        sm.setEmail(body.get("email"));
        sm.setPassword(body.get("password"));
        scrumMasterRepository.save(sm);
        return ResponseEntity.ok(Map.of("id", sm.getId(), "message", "SM created"));
    }

    @PostMapping("/createSprint")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        try {
            String name = body.get("name").toString();
            String goal = body.get("goal").toString();
            LocalDateTime endDate = LocalDateTime.parse(body.get("endDate").toString());

            String sm_Id = body.get("scrumMaster_Id").toString();

            Optional<ScrumMaster> sm = scrumMasterRepository.findById(sm_Id);
            if (sm.isEmpty()){
                return ResponseEntity.status(404).body(Map.of("error", "Scrum Master not found"));
            }

            Sprint sprint = new Sprint(name, goal, endDate, sm.get());
            sprintRepository.save(sprint);

            return ResponseEntity.ok(Map.of(
                    "message", "Sprint Created",
                    "Sprint name : ", name
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/addTask")
    public ResponseEntity<?> addTask(@RequestBody Map<String, Object> body) {
        try {
            String name        = body.get("name").toString();
            String description = body.get("description").toString();
            String priority    = body.get("priority").toString();
            String sprintId    = body.get("sprintId").toString();
            String smId        = body.get("scrumMasterId").toString();
            String userId      = body.get("userId").toString();

            ScrumMaster sm = scrumMasterRepository.findById(smId)
                    .orElseThrow(() -> new RuntimeException("Scrum Master not found"));

            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new RuntimeException("Sprint not found"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Task task = new Task(name, description, TaskPriority.valueOf(priority), sprint, user, sm);

            taskRepository.save(task);

            return ResponseEntity.ok(Map.of("message", "Task created", "taskId", task.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/startSprint/{sprintId}")
    public ResponseEntity<?> startSprint(@PathVariable String sprintId) {
        try {
            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new RuntimeException("Sprint not found"));

            if (sprint.getStatus() != SprintStatus.PLANNING)
                return ResponseEntity.badRequest().body(Map.of("error", "Sprint is not in PLANNING state"));

            sprint.setStatus(SprintStatus.ACTIVE);
            sprint.setStartDate(LocalDateTime.now());
            sprintRepository.save(sprint);

            return ResponseEntity.ok(Map.of("message", "Sprint started"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/completeSprint/{sprintId}")
    public ResponseEntity<?> completeSprint(@PathVariable String sprintId) {
        try {
            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new RuntimeException("Sprint not found"));

            if (sprint.getStatus() != SprintStatus.ACTIVE)
                return ResponseEntity.badRequest().body(Map.of("error", "Sprint is not ACTIVE"));

            sprint.setStatus(SprintStatus.COMPLETED);
            sprintRepository.save(sprint);

            return ResponseEntity.ok(Map.of("message", "Sprint completed"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/addToTeam")
    public ResponseEntity<?> addToTeam(@RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String smId   = body.get("scrumMasterId");

            ScrumMaster sm = scrumMasterRepository.findById(smId)
                    .orElseThrow(() -> new RuntimeException("Scrum Master not found"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setScrumMaster(sm);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "User added to team"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sprints/{scrumMasterId}")
    public ResponseEntity<?> getSprints(@PathVariable String scrumMasterId) {
        List<Sprint> sprints = sprintRepository.findByScrumMaster_Id(scrumMasterId);
        return ResponseEntity.ok(Map.of("sprints", sprints.stream().map(s -> Map.of(
                "id", s.getId(),
                "name", s.getName(),
                "status", s.getStatus(),
                "goal", s.getGoal(),
                "startDate", s.getStartDate() != null ? s.getStartDate().toString() : "not started",
                "endDate", s.getEndDate().toString()
        )).toList()));
    }

    @GetMapping("/team/{smId}")
    public ResponseEntity<?> getTeam(@PathVariable String smId) {
        ScrumMaster sm = scrumMasterRepository.findById(smId)
                .orElseThrow(() -> new RuntimeException("SM not found"));
        return ResponseEntity.ok(Map.of("team", sm.getTeam().stream().map(u -> Map.of(
                "id", u.getId(),
                "name", u.getName()
        )).toList()));
    }
}
