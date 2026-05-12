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

    public ResultConsumerService(CommandRepository commandRepository, ContainerRepository containerRepository, StreamService streamService, RedisService redisService, UserRepository userRepository, MinioService minioService) {
        this.commandRepository = commandRepository;
        this.containerRepository = containerRepository;
        this.streamService = streamService;
        this.redisService = redisService;
        this.userRepository = userRepository;
        this.minioService = minioService;
    }

    @KafkaListener(topics = "results", groupId = "spring-consumer")
    public void consume(Map<String, Object> message){
        System.out.println("[Result received] " + message);

        String type = (String) message.get("type");
        if (type.equals("EXECUTE_COMMAND")){
            consumeCommands(message);
        } else if (type.equals("CREATE_CONTAINER")) {
            try {
                consumeContainer(message);
            } catch (IOException e) {
                System.err.println("ERROR (container creation) : " + e);
            }
        }
    }

    private void consumeCommands(Map<String, Object> message){
        String commandId = (String) message.get("commandId");
        String output    = (String) message.get("output");

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

    private void consumeContainer(Map<String, Object> message) throws IOException {
        System.out.println("consumeContainer called for: " + message.get("requestId"));
        String requestId = (String) message.get("requestId");
        String containerId = (String) message.get("containerId");
        String containerName = (String) message.get("name");
        String userId = (String) message.get("userId");
        User containerUser = userRepository.findById(userId).orElse(null);

        Container container = new Container(containerId, containerName, containerUser);
        container.setStatus("RUNNING");
        System.out.println(container.getOwner());
        redisService.setContainerTTL(containerId);
        containerRepository.save(container);

        SseEmitter emitter = waitForEmitter(requestId, 500);
        System.out.println("Emitter found: " + (emitter != null));

        if (emitter != null) {
            try {
                System.out.println("Sending to emitter: " + requestId);
                emitter.send(SseEmitter.event().data(Map.of(
                        "containerId", containerId,
                        "name", containerName
                )));
                System.out.println("Send successful, completing...");
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitter.completeWithError(e);
            }
        }
    }

    private SseEmitter waitForEmitter(String requestId, int timeoutMs) {
        System.out.println("Waiting for emitter: " + requestId);
        int waited = 0;
        int interval = 100;

        while (waited < timeoutMs) {
            SseEmitter emitter = streamService.emitters.get(requestId);
            if (emitter != null) return emitter;

            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }

            waited += interval;
        }

        return null;
    }
}
