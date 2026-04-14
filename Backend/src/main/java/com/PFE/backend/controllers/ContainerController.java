package com.PFE.backend.controllers;

import com.PFE.backend.models.Container;
import com.PFE.backend.services.ContainerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/containers")
@CrossOrigin(origins = "*")
public class ContainerController {

    private final ContainerService containerService;

    public ContainerController(ContainerService containerService){
        this.containerService = containerService;
    }

    @GetMapping
    public List<Container> getAll(){
        return containerService.getAll();
    }

    @PostMapping
    public ResponseEntity<Container> create(@RequestBody Map<String, String> body){
        String id = body.get("id");
        String name = body.get("name");
        Container saved = containerService.save(id, name);
        return ResponseEntity.ok(saved);
    }

    //the delete is inside RedisController.java
}
