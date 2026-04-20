package com.PFE.backend.controllers;

import com.PFE.backend.models.Container;

import com.PFE.backend.services.ContainerService;
import com.PFE.backend.services.DockerService;
import com.PFE.backend.services.RedisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/containers")
@CrossOrigin(origins = "*")
public class DockerController {

    private final DockerService dockerServices;
    private final ContainerService containerService;
    private final RedisService redisService;

    public DockerController(DockerService dockerServices, ContainerService containerService, RedisService redisService) {
        this.dockerServices = dockerServices;
        this.containerService = containerService;
        this.redisService = redisService;
    }

    @PostMapping("/run")
    public ResponseEntity<?> run(@RequestBody Map<String, String> body){
        int memory = Integer.parseInt(body.get("memory"));
        double cpu = Double.parseDouble(body.get("cpu"));

        String containerID = dockerServices.runContainer(memory,cpu);
        String name = dockerServices.getContainerName(containerID);
        redisService.setContainer(containerID);
        Container container = containerService.save(containerID, name);

        return ResponseEntity.ok(Map.of("message", "container started", "containerID", containerID));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestBody Map<String, String> body){
        String containerID = body.get("containerID");

        dockerServices.stopContainer(containerID);
        redisService.deleteContainer(containerID);

        return ResponseEntity.ok(Map.of("message", "container stopped"));
    }
}
