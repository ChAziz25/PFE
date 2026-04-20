package com.PFE.backend.services;

import com.PFE.backend.models.Command;
import com.PFE.backend.models.Container;
import com.PFE.backend.repositories.CommandRepository;
import com.PFE.backend.repositories.ContainerRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ResultConsumerService {

    private final CommandRepository commandRepository;
    private final ContainerRepository containerRepository;
    private final StreamService streamService;

    public ResultConsumerService(CommandRepository commandRepository, ContainerRepository containerRepository, StreamService streamService) {
        this.commandRepository = commandRepository;
        this.containerRepository = containerRepository;
        this.streamService = streamService;
    }

    @KafkaListener(topics = "results", groupId = "spring-consumer")
    public void consume(Map<String, Object> message){
        System.out.println("[Result received] " + message);

        String commandId = (String) message.get("commandId");
        String output    = (String) message.get("output");

        Command cmd = commandRepository.findById(commandId).orElse(null);

        if (cmd == null) {
            System.err.println("⚠️ Command not found: " + commandId);
            return;
        }

        Container container = containerRepository.findById(cmd.getContainerId()).orElse(null);
        if (container != null) {
            container.setLastUsed(LocalDateTime.now());
            containerRepository.save(container);
        }

        if (!"PENDING".equals(cmd.getStatus())) {
            return;
        } else if (output.toLowerCase().contains("error")) {
            cmd.setStatus("FAILED");
        } else {
            cmd.setStatus("DONE");
        }

        cmd.setOutput(output);
        commandRepository.save(cmd);

        SseEmitter emitter = streamService.emitters.get(commandId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(output));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }

        System.out.println("Command " + commandId + " output: "+ output);
    }
}
