package com.PFE.backend.services;

import com.PFE.backend.models.User;
import com.PFE.backend.repositories.ContainerRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ContainerService {

    private final ContainerRepository containerRepository;
    private final RedisService redisService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ContainerService(ContainerRepository containerRepository, RedisService redisService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.containerRepository = containerRepository;
        this.redisService = redisService;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void runContainer(int memory, double cpu, String requestId, User user) throws Exception{

        Map<String, Object> payload = Map.of(
                "type", "CREATE_CONTAINER",
                "requestId", requestId,
                "memory", memory,
                "cpu", cpu,
                "userId", user.getId()
        );
        System.out.println("Sending payload: " + payload);

        kafkaTemplate.send("commands", payload);
    }

}
