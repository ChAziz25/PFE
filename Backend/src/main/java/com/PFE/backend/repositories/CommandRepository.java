package com.PFE.backend.repositories;

import com.PFE.backend.models.Command;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CommandRepository extends JpaRepository<Command, String> {
    List<Command> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);
}
