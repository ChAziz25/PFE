package com.PFE.backend.services;

import com.PFE.backend.models.Container;
import com.PFE.backend.models.User;
import com.PFE.backend.repositories.ContainerRepository;
import com.PFE.backend.repositories.UserRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.HostConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Service
public class ContainerService {

    private final ContainerRepository containerRepository;
    private final RedisService redisService;
    private final DockerClient dockerClient;
    private final StreamService streamService;


    public ContainerService(ContainerRepository containerRepository, RedisService redisService, KafkaTemplate<String, Object> kafkaTemplate, DockerClient dockerClient, UserRepository userRepository, StreamService streamService) {
        this.containerRepository = containerRepository;
        this.redisService = redisService;
        this.dockerClient = dockerClient;
        this.streamService = streamService;
    }

    public String runContainer(int memory, double cpu, User user){
        long memoryInBytes = memory * 1024 * 1024L;
        long nanoCpus = (long) (cpu * 1000000000L);

        System.out.println("> Creating container with memory " + memory + " and CPU " + cpu);
        System.out.println("> userId received: " + user);

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(memoryInBytes)
                .withNanoCPUs(nanoCpus);

        try {
            CreateContainerResponse containerCreation = dockerClient.createContainerCmd("pfe-sandbox")
                    .withHostConfig(hostConfig)
                    .exec();

            String containerId = containerCreation.getId();
            dockerClient.startContainerCmd(containerId).exec();

            String name = null;

            if (waitForReady(containerId, 10)) {
                name = getContainerName(containerId);
            }

            Container container = new Container(containerId, name, user);
            container.setStatus("RUNNING");
            System.out.println(container.getOwner());
            redisService.setContainerTTL(containerId);
            containerRepository.save(container);

            return containerId;
        } catch (DockerException e) {
            System.err.println("Failed to execute Docker command: " + e.getMessage());
            return e.getMessage();
        }
    }

    private boolean waitForReady(String containerId, int timeOut){
        for (int i=0; i<timeOut; i++){
            try {
                InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
                String status = inspect.getState().getStatus();
                if ("running".equals(status)) { return true; }
            } catch (DockerException e) {
                System.out.println("Container metadata socket not ready yet, retrying...");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }}
        return false;
    }

    private String getContainerName(String containerId) {
        try {
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            String rawName = inspect.getName();

            if (rawName != null && rawName.startsWith("/")) {
                return rawName.substring(1);
            }
            return rawName;
        } catch (DockerException e) {
            return null;
        }
    }
}
