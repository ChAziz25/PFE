package com.PFE.backend.controllers;

import com.PFE.backend.models.Container;
import com.PFE.backend.models.User;
import com.PFE.backend.repositories.CommandRepository;
import com.PFE.backend.repositories.ContainerRepository;
import com.PFE.backend.repositories.UserRepository;
import com.PFE.backend.services.*;

import com.github.dockerjava.api.DockerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private final DockerClient dockerClient;
    private final UserRepository userRepository;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Autowired
    private final AiAgentService aiAgentService;

    public ContainerController(ContainerService containerService, ContainerRepository containerRepository, CommandProducerService commandProducerService, CommandRepository commandRepository, StreamService streamService, RedisService redisService, RedisTemplate<String, String> redisTemplate, DockerClient dockerClient, UserRepository userRepository, AiAgentService aiAgentService) {
        this.containerService = containerService;
        this.containerRepository = containerRepository;
        this.commandProducerService = commandProducerService;
        this.commandRepository = commandRepository;
        this.streamService = streamService;
        this.redisService = redisService;
        this.redisTemplate = redisTemplate;
        this.dockerClient = dockerClient;
        this.userRepository = userRepository;
        this.aiAgentService = aiAgentService;
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
            String userId      = body.get("userId");
            String provider    = body.get("provider");

            if (command.startsWith("/ask")) {
                String question = command.replaceFirst("^/ask(\\(\\w+\\))?\\s*", "");
                String aiResponse = aiAgentService.ask(provider, question, userId, containerId);
                return ResponseEntity.ok(Map.of("response", aiResponse, "type", "ai"));
            }

            String commandId = commandProducerService.sendCommand(containerId, command, "USER");

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
            dockerClient.startContainerCmd(containerId).exec();

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
            dockerClient.stopContainerCmd(containerId).exec();

            Container container = containerRepository.findById(containerId).orElse(null);
            if (container != null) {
                container.setStatus("STOPPED");
                containerRepository.save(container);
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
                    emitter.send(SseEmitter.event().name("ttl").data(ttl.toString()));
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
                schedule.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);

        return emitter;
    }

    @DeleteMapping("/deleteContainer")
    @Transactional
    public ResponseEntity<?> deleteContainer(@RequestBody Map<String, String> body){
        String containerId = body.get("containerId");

        if (containerId == null || containerId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "containerId is required"
            ));
        }

        try {
            dockerClient.removeContainerCmd(containerId).exec();
            Container container = containerRepository.findById(containerId)
                    .orElseThrow(() -> new RuntimeException("Container not found in DB"));

            User owner = container.getOwner();
            owner.getContainers().remove(container);
            userRepository.save(owner);

            containerRepository.delete(container);

            return ResponseEntity.ok(Map.of(
                    "message", "container deleted",
                    "containerId", containerId
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
