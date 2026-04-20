package com.PFE.backend.services;

import com.PFE.backend.models.Container;
import com.PFE.backend.repositories.ContainerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContainerService {

    private final ContainerRepository containerRepository;

    public ContainerService(ContainerRepository containerRepository){
        this.containerRepository = containerRepository;
    }

    public Container save(String id, String name){
        Container container = new Container();
        container.setId(id);
        container.setName(name);
        return containerRepository.save(container);
    }

    public List<Container> getAll(){
        return containerRepository.findAll();
    }

    public Optional<Container> getById(String id){
        return containerRepository.findById(id);
    }

    public void delete(String id){
        containerRepository.deleteById(id);
    }
}
