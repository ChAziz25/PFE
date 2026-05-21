package com.PFE.backend.controllers;

import com.PFE.backend.models.Container;
import com.PFE.backend.models.Secret;
import com.PFE.backend.models.Tool;
import com.PFE.backend.models.User;
import com.PFE.backend.repositories.ContainerRepository;
import com.PFE.backend.repositories.SecretRepository;
import com.PFE.backend.repositories.ToolsRepository;
import com.PFE.backend.repositories.UserRepository;
import com.PFE.backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final SecretRepository secretRepository;
    private final ContainerRepository containerRepository;
    private final ToolsRepository toolsRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserController(UserService userService, UserRepository userRepository, SecretRepository secretRepository, ContainerRepository containerRepository, ToolsRepository toolsRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.secretRepository = secretRepository;
        this.containerRepository = containerRepository;
        this.toolsRepository = toolsRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        try {
            String name = body.get("name").toString();
            String email = body.get("email").toString();
            String password = body.get("password").toString();

            userService.createUser(name, email, password);

            return ResponseEntity.ok(Map.of(
                    "message", "User Created",
                    "User name : ", name
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        try{
            String email = body.get("email").toString();
            String password = body.get("password").toString();

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }

            User user = userOpt.get();
            if (!user.getPassword().equals(password)) {
                return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestParam String userId){
        return userRepository.findById(userId).map(user -> ResponseEntity.ok(Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "secrets", user.getSecrets().stream().map(s -> Map.of(
                        "id", s.getId(),
                        "name", s.getName(),
                        "value", s.getValue()
                )).toList(),
                "containers", user.getContainers().stream().map(c -> Map.of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "status", c.getStatus()
                )).toList()
        ))).orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "user not found")));
    }

    @PostMapping("/addSecret")
    public ResponseEntity<?> addSecret(@RequestBody Map<String, Object> body) {
        try {
            String userId = body.get("userId").toString();
            String name   = body.get("name").toString();
            String value  = body.get("value").toString();

            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            Secret secret = new Secret(name, value, user.get());
            secretRepository.save(secret);

            return ResponseEntity.ok(Map.of(
                    "message", "Secret Created",
                    "Secret name : ", name
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/deleteSecret")
    @Transactional
    public ResponseEntity<?> deleteSecret(@RequestBody Map<String, String> body){
        String secretId = body.get("secretId");

        if (secretId == null || secretId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "containerId is required"
            ));
        }

        try {
            Secret secret = secretRepository.findById(secretId)
                    .orElseThrow(() -> new RuntimeException("Container not found in DB"));

            User user = secret.getUser();
            user.getSecrets().remove(secret);
            userRepository.save(user);

            secretRepository.delete(secret);

            return ResponseEntity.ok(Map.of(
                    "message", "container deleted",
                    "containerId", secretId
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/addTool")
    public ResponseEntity<?> addTool(@RequestBody Map<String, Object> body){
        try {
            String userId = (String) body.get("userId");
            String script = (String) body.get("script");
            String type   = (String) body.get("type");
            List<String> containers = (List<String>) body.get("containers");

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            Tool tool = new Tool(script, type, user);

            List<Container> resolvedContainers = new ArrayList<>();
            for (String containerId : containers) {
                Optional<Container> container = containerRepository.findById(containerId);
                if (container.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "container " + containerId + " doesn't exist."
                    ));
                }
                resolvedContainers.add(container.get());
            }
            tool.setContainers(resolvedContainers);

            toolsRepository.save(tool);

            Map<String, Object> payload = Map.of(
                    "type", "NEW_TOOL",
                    "script", script,
                    "targetContainerIds", resolvedContainers.stream()
                            .map(Container::getId)
                            .collect(Collectors.toList()),
                    "script_type", type
            );

            kafkaTemplate.send("commands", payload);

            return ResponseEntity.ok(Map.of(
                    "message", "tool added successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
