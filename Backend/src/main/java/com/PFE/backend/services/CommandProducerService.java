package com.PFE.backend.services;

import com.PFE.backend.models.Command;
import com.PFE.backend.repositories.CommandRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class CommandProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CommandRepository commandRepository;

    public CommandProducerService(KafkaTemplate<String, Object> kafkaTemplate, CommandRepository commandRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.commandRepository = commandRepository;
    }

    public String sendCommand(String containerId, String command) {
        String commandId = UUID.randomUUID().toString();

        Command cmd = new Command();
        cmd.setId(commandId);
        cmd.setContainerId(containerId);
        cmd.setCommand(command);
        cmd.setStatus("PENDING");
        commandRepository.save(cmd);

        Map<String, Object> payload = Map.of(
                "commandId", commandId,
                "command", command,
                "targetContainerId", containerId
        );

        kafkaTemplate.send("commands", payload);

        return commandId;
    }
}
