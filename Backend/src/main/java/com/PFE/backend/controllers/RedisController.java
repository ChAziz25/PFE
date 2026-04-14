package com.PFE.backend.controllers;

import com.PFE.backend.models.Container;
import com.PFE.backend.services.ContainerService;
import com.PFE.backend.services.RedisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/containers")
@CrossOrigin(origins = "*")
public class RedisController {

    private final ContainerService containerService;
    private final RedisService redisService;

    public RedisController(ContainerService containerService, RedisService redisService) {
        this.containerService = containerService;
        this.redisService = redisService;
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable String id){
        if (redisService.containerExists(id)){
            redisService.updateHeartbeat(id);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Container> start(@PathVariable String id, @RequestBody Map<String, String> body){
        String name = body.get("name");
        redisService.setContainer(id);
        Container saved = containerService.save(id, name);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id){
        containerService.delete(id);
        redisService.deleteContainer(id);
        return ResponseEntity.ok().build();
    }
}
