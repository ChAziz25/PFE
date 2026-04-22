package com.PFE.backend.services;

import com.PFE.backend.repositories.ContainerRepository;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class RedisExpirationListener implements MessageListener {
    private final ContainerRepository containerRepository;

    public RedisExpirationListener(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
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
            ProcessBuilder pb = new ProcessBuilder("docker", "stop", containerId);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                containerRepository.findById(containerId).ifPresent(container -> {
                    container.setStatus("STOPPED");
                    containerRepository.save(container);
                });
            } else {
                System.err.println("docker stop exited with code: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Failed to stop container: " + e.getMessage());
        }
    }
}
