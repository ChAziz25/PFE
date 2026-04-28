package com.PFE.backend.repositories;

import com.PFE.backend.models.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommandRepository extends JpaRepository<Command, String> {
    List<Command> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);
}
