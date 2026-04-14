package com.PFE.backend.services;

import com.PFE.backend.repositories.ContainerRepository;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {

    private final ContainerRepository containerRepository;
    private Thread dockerContainer;

    public KafkaService(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
        dockerContainer = new startNewContainer();
    }
}

class startNewContainer extends Thread{
    
}
