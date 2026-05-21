package com.PFE.backend.services;

import com.PFE.backend.models.Command;
import com.PFE.backend.models.Container;
import com.PFE.backend.models.User;
import com.PFE.backend.repositories.CommandRepository;
import com.PFE.backend.repositories.ContainerRepository;
import com.PFE.backend.repositories.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ResultConsumerService {

    private final CommandRepository commandRepository;
    private final ContainerRepository containerRepository;
    private final StreamService streamService;
    private final RedisService redisService;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final PendingResultService pendingResultService;

    public ResultConsumerService(CommandRepository commandRepository, ContainerRepository containerRepository, StreamService streamService, RedisService redisService, UserRepository userRepository, MinioService minioService, PendingResultService pendingResultService) {
        this.commandRepository = commandRepository;
        this.containerRepository = containerRepository;
        this.streamService = streamService;
        this.redisService = redisService;
        this.userRepository = userRepository;
        this.minioService = minioService;
        this.pendingResultService = pendingResultService;
    }

    @KafkaListener(topics = "results", groupId = "spring-consumer")
    public void consume(Map<String, Object> message){
        System.out.println("[Result received] " + message);

        String type = (String) message.get("type");
        if (type.equals("EXECUTE_COMMAND")){
            consumeCommands(message);
        } else if (type.equals("NEW_TOOL")){
            System.out.println("[Tool deployed] " + message.get("script_type"));
        } else {
            System.out.println("[Unknown message type] " + type);
        }
    }

    private void consumeCommands(Map<String, Object> message){
        String commandId = (String) message.get("commandId");
        String output    = (String) message.get("output");
        String source    = (String) message.get("source");

        Command cmd = commandRepository.findById(commandId).orElse(null);

        if (cmd == null) {
            System.err.println("⚠️ Command not found: " + commandId);
            return;
        }

        containerRepository.findById(cmd.getContainerId()).ifPresent(container -> {
            container.setLastUsed(LocalDateTime.now());
            redisService.setContainerTTL(container.getId());
            containerRepository.save(container);
        });

        if (!"PENDING".equals(cmd.getStatus())) {
            return;
        } else if (output.toLowerCase().contains("error")) {
            cmd.setStatus("FAILED");
        } else {
            cmd.setStatus("DONE");
        }

        cmd.setOutput(output);
        commandRepository.save(cmd);

        storeOutputInMinIO(commandId, output);

        if ("AI".equals(source)){
            pendingResultService.complete(commandId, output);
        }else {
            SseEmitter emitter = streamService.emitters.get(commandId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().data(output));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        }

        System.out.println("Command " + commandId + " output: "+ output);
    }

    private void storeOutputInMinIO(String commandId, String output) {
        try {
            Command command = commandRepository.findById(commandId)
                    .orElseThrow(() -> new RuntimeException("Command not found"));

            Container container = containerRepository.findById(command.getContainerId())
                    .orElseThrow(() -> new RuntimeException("Container not found"));

            String user = container.getOwner().getName();

            String filename = user + "/" + commandId + "_output.txt";
            byte[] outputBytes = output.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(outputBytes);

            minioService.uploadFile(filename, inputStream, outputBytes.length);
        } catch (Exception e) {
            System.err.println("Error storing output in MinIO: " + e.getMessage());
        }
    }
}
