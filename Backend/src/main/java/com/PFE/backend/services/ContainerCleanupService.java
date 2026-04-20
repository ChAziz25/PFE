package com.PFE.backend.services;

import com.PFE.backend.models.Container;
import com.PFE.backend.repositories.ContainerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContainerCleanupService {
    private final ContainerRepository containerRepository;
    private final Container container;

    public ContainerCleanupService(ContainerRepository containerRepository, Container container) {
        this.containerRepository = containerRepository;
        this.container = container;
    }

    @Scheduled(fixedRate = 60000)
    public void cleanup(){
        List<Container> containers = containerRepository.findAll();

        for (Container container : containers) {
            if (container.getLastUsed() == null) continue;

            long minutes = java.time.Duration.between(
                    container.getLastUsed(),
                    LocalDateTime.now()
            ).toMinutes();

            if (minutes > 5) {
                try {
                    ProcessBuilder pbCheck = new ProcessBuilder(
                            "docker", "inspect", "-f", "{{.State.Running}}", container.getId()
                    );

                    Process processCheck = pbCheck.start();
                    processCheck.waitFor();

                    BufferedReader checkReader = new BufferedReader(
                            new InputStreamReader(processCheck.getInputStream())
                    );

                    String isRunning = checkReader.readLine();

                    if (!"true".equals(isRunning)){
                        continue;
                    }

                    System.out.println("Cleaning container:" + container.getId());

                    ProcessBuilder pb = new ProcessBuilder(
                            "docker", "stop", container.getId()
                    );

                    Process process = pb.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        container.setStatus("STOPPED");
                        containerRepository.save(container);
                    } else {
                        System.err.println("Failed to stop: " + container.getId());
                    }
                } catch (Exception e) {
                    System.err.println("Cleanup failed : " + e.getMessage());
                }
            }
        }
    }
}
