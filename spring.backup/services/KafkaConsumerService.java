package com.PFE.backend.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class KafkaConsumerService {
    private final ExecService execService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaConsumerService(ExecService execService) {
        this.execService = execService;
    }

    @KafkaListener(topics = "results", groupId = "sandbox-results")
    public void listen(String message) {
        try {
            Map<String, String> data = objectMapper.readValue(message, Map.class);

            String commandID = data.get("commandID");
            String output = data.get("output");

            execService.complete(commandID, output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
