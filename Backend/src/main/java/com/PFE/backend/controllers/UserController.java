package com.PFE.backend.controllers;

import com.PFE.backend.models.Container;
import com.PFE.backend.models.Secret;
import com.PFE.backend.models.User;
import com.PFE.backend.repositories.SecretRepository;
import com.PFE.backend.repositories.UserRepository;
import com.PFE.backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final SecretRepository secretRepository;

    public UserController(UserService userService, UserRepository userRepository, SecretRepository secretRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.secretRepository = secretRepository;
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
                "secrets", user.getSecrets(),
                "containers", user.getContainers()
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
}
