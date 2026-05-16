package com.PFE.backend.services;

import com.PFE.backend.repositories.ContainerRepository;
import com.github.dockerjava.api.DockerClient;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class RedisExpirationListener implements MessageListener {
    private final ContainerRepository containerRepository;
    private final DockerClient dockerClient;

    public RedisExpirationListener(ContainerRepository containerRepository, DockerClient dockerClient) {
        this.containerRepository = containerRepository;
        this.dockerClient = dockerClient;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());

        if (!key.startsWith("container:")) {
            return;
        }

        String containerId = key.replace("container:", "");
        System.out.println("TTL -> stopping container:" + containerId);

        try {
            dockerClient.pauseContainerCmd(containerId).exec();
            containerRepository.findById(containerId).ifPresent(container -> {
                container.setStatus("PAUSED");
                containerRepository.save(container);
            });
        } catch (Exception e) {
            System.err.println("Failed to stop container: " + e.getMessage());
        }
    }
}
