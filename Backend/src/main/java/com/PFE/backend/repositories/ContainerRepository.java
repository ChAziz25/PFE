package com.PFE.backend.repositories;

import com.PFE.backend.models.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContainerRepository extends JpaRepository<Container, String> {
    List<Container> findByOwner_Id(String userId);
}
