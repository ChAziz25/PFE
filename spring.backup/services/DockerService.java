package com.PFE.backend.services;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class DockerService {

    public String runContainer(int memory, double cpu){
        try {
            ProcessBuilder pb = new ProcessBuilder("docker",
                    "run",
                    "-d",
                    "-p", "8080:8080",
                    "--memory=" + memory + "mb",
                    "--cpus=" + cpu,
                    "pfe-sandbox"
            );
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String containerID = reader.readLine();
            process.waitFor();

            if (containerID == null || containerID.isEmpty()) {
                throw new RuntimeException("Failed to start container");
            }

            return containerID.trim();
        } catch (Exception e) {
            throw new RuntimeException("Docker run failed: " + e.getMessage());
        }
    }

    public void stopContainer(String containerID){
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "stop", containerID);
            Process process = pb.start();

            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Docker stop failed: " + e.getMessage());
        }
    }

    public String getContainerName(String containerID){
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "inspect", "--format", "{{.name}}", containerID);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String name = reader.readLine();
            process.waitFor();

            return name != null ? name.replace("/", "") : null;
        } catch (Exception e) {
            throw new RuntimeException("Inspect failed: " + e.getMessage());
        }
    }
}
