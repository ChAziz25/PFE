package com.PFE.backend.controllers;

import com.PFE.backend.models.Container;
import com.PFE.backend.repositories.CommandRepository;
import com.PFE.backend.repositories.ContainerRepository;
import com.PFE.backend.services.CommandProducerService;
import com.PFE.backend.services.ContainerService;
import com.PFE.backend.services.StreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class ContainerController {
    private final ContainerService containerService;
    private final ContainerRepository containerRepository;
    private final CommandProducerService commandProducerService;
    private final CommandRepository commandRepository;
    private final StreamService streamService;

    public ContainerController(ContainerService containerService, ContainerRepository containerRepository, CommandProducerService commandProducerService, CommandRepository commandRepository, StreamService streamService) {
        this.containerService = containerService;
        this.containerRepository = containerRepository;
        this.commandProducerService = commandProducerService;
        this.commandRepository = commandRepository;
        this.streamService = streamService;
    }

    @PostMapping("/run")
    public ResponseEntity<?> run (@RequestBody Map<String, Object> body){
        try {
            int memory = Integer.parseInt(body.get("memory").toString());
            double cpu = Double.parseDouble(body.get("cpu").toString());

            String containerId = containerService.runContainer(memory, cpu);

            return ResponseEntity.ok(Map.of(
                    "message", "container started",
                    "containerId", containerId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/exec")
    public ResponseEntity<?> exec(@RequestBody Map<String, String> body) {
        try {
            String containerId = body.get("containerId");
            String command     = body.get("command");

            String commandId = commandProducerService.sendCommand(containerId, command);

            Container container = containerRepository.findById(containerId).orElse(null);
            if (container != null) {
                container.setLastUsed(LocalDateTime.now());
                containerRepository.save(container);
            }

            return ResponseEntity.ok(Map.of("commandId", commandId, "status", "sent"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/result/{id}")
    public ResponseEntity<?> getResult(@PathVariable String id) {
        return commandRepository.findById(id).map(cmd -> ResponseEntity.ok(Map.of(
                "status", cmd.getStatus(),
                "output", cmd.getOutput()
        ))).orElseGet(() -> ResponseEntity.status(404).body(
                Map.of("error", "Command not found")
        ));
    }

    @GetMapping(value = "/stream/{commandId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String commandId){
        return streamService.createEmitter(commandId);
    }

    @GetMapping("/listContainers")
    public ResponseEntity<?> listContainers() {
        List<Container> containers = containerRepository.findAll();

            List<String> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();

            for (Container c : containers) {
                ids.add(c.getId());
                names.add(c.getName());
            }

            return ResponseEntity.ok(Map.of(
                    "containerListID", ids, "containerList", names
            ));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startContainer(@RequestBody Map<String, String> body) {
        String containerId = body.get("containerId");

        if (containerId == null || containerId.isEmpty()){
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "containerId is required"
            ));
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "start", containerId
            );

            Process process = pb.start();
            process.waitFor();

            String error = new String(process.getErrorStream().readAllBytes());
            if (!error.isEmpty()){
                return ResponseEntity.status(500).body(Map.of("error", error));
            }

            Container container = containerRepository.findById(containerId).orElse(null);
            if (container != null) {
                container.setLastStartedAt(LocalDateTime.now());
                container.setLastUsed(LocalDateTime.now());
                container.setStatus("RUNNING");
                containerRepository.save(container);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "container started",
                    "containerId", containerId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopContainer(@RequestBody Map<String, String> body) {
        String containerId = body.get("containerId");

        if (containerId == null || containerId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "containerId is required"
            ));
        }

        try {
            ProcessBuilder stopPb = new ProcessBuilder(
                    "docker", "stop", containerId
            );

            Process stopProcess = stopPb.start();
            stopProcess.waitFor();

            String error = new String(stopProcess.getErrorStream().readAllBytes());
            if (!error.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of("error", error));
            }

            Container container = containerRepository.findById(containerId).orElse(null);
            if (container != null) {
                container.setStatus("STOPPED");
            }

            return ResponseEntity.ok(Map.of(
                    "message", "container stopped",
                    "containerId", containerId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteContainer(@RequestBody Map<String, String> body){
        String containerId = body.get("containerId");

        if (containerId == null || containerId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "containerId is required"
            ));
        }

        try {
            ProcessBuilder rmPb = new ProcessBuilder(
                    "docker", "rm", "-f", containerId
            );

            Process process = rmPb.start();
            process.waitFor();

            containerRepository.deleteById(containerId);

            return ResponseEntity.ok(Map.of(
                    "message", "container deleted",
                    "containerId", containerId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
