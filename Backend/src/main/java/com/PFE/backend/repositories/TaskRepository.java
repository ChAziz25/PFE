package com.PFE.backend.repositories;

import com.PFE.backend.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findBySprint_Id(String sprintId);
    List<Task> findByTaskFor_Id(String userId);
}
