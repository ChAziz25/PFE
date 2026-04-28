package com.PFE.backend.controllers;

import com.PFE.backend.models.Container;
import com.PFE.backend.models.User;
import com.PFE.backend.repositories.CommandRepository;
import com.PFE.backend.repositories.ContainerRepository;
import com.PFE.backend.services.CommandProducerService;
import com.PFE.backend.services.ContainerService;
import com.PFE.backend.services.RedisService;
import com.PFE.backend.services.StreamService;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class ContainerController {
    private final ContainerService containerService;
    private final ContainerRepository containerRepository;
    private final CommandProducerService commandProducerService;
    private final CommandRepository commandRepository;
    private final StreamService streamService;
    private final RedisService redisService;
    private final RedisTemplate<String, String> redisTemplate;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public ContainerController(ContainerService containerService, ContainerRepository containerRepository, CommandProducerService commandProducerService, CommandRepository commandRepository, StreamService streamService, RedisService redisService, RedisTemplate<String, String> redisTemplate) {
        this.containerService = containerService;
        this.containerRepository = containerRepository;
        this.commandProducerService = commandProducerService;
        this.commandRepository = commandRepository;
        this.streamService = streamService;
        this.redisService = redisService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/run")
    public ResponseEntity<?> run (@RequestBody Map<String, Object> body){
        try {
            ObjectMapper mapper = new ObjectMapper();

            int memory = Integer.parseInt(body.get("memory").toString());
            double cpu = Double.parseDouble(body.get("cpu").toString());
            User user = mapper.convertValue(body.get("user"), User.class);

            String requestId = "cmd-" + UUID.randomUUID();
            containerService.runContainer(memory, cpu, requestId, user);

            return ResponseEntity.ok(Map.of(
                    "message", "container started",
                    "requestedId", requestId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping(value = "/containers/stream", produces = "text/event-stream")
    public SseEmitter streamContainer(@RequestParam String requestId) {
        SseEmitter emitter = new SseEmitter(0L);
        streamService.emitters.put(requestId, emitter);
        System.out.println("Emitter registered: " + requestId);

        try{
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        emitter.onCompletion(() -> streamService.emitters.remove(requestId));
        emitter.onTimeout(() -> streamService.emitters.remove(requestId));

        return emitter;
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
                redisService.setContainerTTL(containerId);
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
    public ResponseEntity<?> listContainers(@RequestParam String userId) {
        List<Map<String, String>> containers = new ArrayList<>();

        for (Container c : containerRepository.findByOwner_Id(userId)) {
            containers.add(Map.of("id", c.getId(), "name", c.getName()));
        }

        return ResponseEntity.ok(Map.of("containerList", containers));
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
                redisService.setContainerTTL(containerId);
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

    @GetMapping("/container-status/{containerId}")
    public SseEmitter streamStatus(@PathVariable String containerId) {
        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L);
        emitters.put(containerId, emitter);

        emitter.onCompletion(() -> emitters.remove(containerId));
        emitter.onTimeout(() -> emitters.remove(containerId));

        ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
        schedule.scheduleAtFixedRate(() -> {
            try {
                Long ttl = redisTemplate.getExpire("container:" + containerId, TimeUnit.SECONDS);

                if (ttl == null || ttl <= 0) {
                    emitter.send(SseEmitter.event().name("stopped").data("STOPPED"));
                    emitter.complete();
                    schedule.shutdown();
                } else {
                    emitter.send(SseEmitter.event().name("ttl").data("STOPPED"));
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
                schedule.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);

        return emitter;
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
