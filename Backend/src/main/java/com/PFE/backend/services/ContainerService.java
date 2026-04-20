package com.PFE.backend.services;

import com.PFE.backend.models.Container;
import com.PFE.backend.repositories.ContainerRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class ContainerService {

    private final ContainerRepository containerRepository;

    public ContainerService(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }


    public String runContainer(int memory, double cpu) throws Exception{
        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "run", "-d",
                "--memory=" + memory + "mb",
                "--cpus=" + cpu,
                "pfe-sandbox"
        );
        Process process = processBuilder.start();

        String output = new String(process.getInputStream().readAllBytes());
        String error  = new String(process.getErrorStream().readAllBytes());

        int exitCode = process.waitFor();
        if (exitCode != 0 || !error.isEmpty()){
            throw new RuntimeException("Docker error: " + error);
        }

        String containerId = output.trim();
        waitForReady(containerId);

        String containerName = getContainerName(containerId);
        System.out.println("Container Name: " + containerName);

        Container container = new Container(containerId, containerName);
        container.setStatus("RUNNING");
        containerRepository.save(container);

        return containerId;
    }

    private void waitForReady(String containerId) throws Exception {
        ProcessBuilder logsProcess = new ProcessBuilder( "docker", "logs", "-f", containerId );
        Process process = logsProcess.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[Container Logs] " + line);

            if (line.contains("READY")){
                System.out.println("Container is ready");
                process.destroy();
                break;
            }
        }
    }

    private String getContainerName(String containerId) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "inspect", "--format", "{{.Name}}", containerId
        );
        Process process = processBuilder.start();

        String output = new String(process.getInputStream().readAllBytes());
        String error  = new String(process.getErrorStream().readAllBytes());

        int exitCode = process.waitFor();
        if (exitCode != 0 || !error.isEmpty()){
            throw new RuntimeException("Inspect error: " + error);
        }

        return output.trim().replace("/", "");
    }
}
