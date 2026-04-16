package com.PFE.backend.services;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class ExecService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<String, CompletableFuture<String>> pendingResult = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public String execute(String command, String containerID) throws Exception{
        String commandID = "cmd-" + UUID.randomUUID();

        CompletableFuture<String> future = new CompletableFuture<>();
        pendingResult.put(commandID, future);

        Map<String, String> payloadMap = Map.of(
                "commandId", commandID,
                "command", command,
                "targetContainerId", containerID
        );

        String payload = objectMapper.writeValueAsString(payloadMap);

        kafkaTemplate.send("commands", payload);

        try {
            return future.get(10, TimeUnit.SECONDS);
        }catch (TimeoutException e){
            pendingResult.remove(commandID);
            throw new RuntimeException("Execution timeout");
        }
    }

    public void complete(String commandID, String output){
        System.out.println("received");
        if (commandID == null) {
            System.out.println("❌ Received null commandID from Kafka");
            return;
        }

        CompletableFuture<String> future = pendingResult.get(commandID);

        if (future != null) {
            future.complete(output);
            pendingResult.remove(commandID);
        }else {
            System.out.println("⚠ Unknown commandID: " + commandID);
        }
    }
}
