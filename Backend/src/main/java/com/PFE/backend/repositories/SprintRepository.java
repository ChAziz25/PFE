package com.PFE.backend.repositories;

import com.PFE.backend.models.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, String> {
    List<Sprint> findByScrumMaster_Id(String scrumMasterId);
}
