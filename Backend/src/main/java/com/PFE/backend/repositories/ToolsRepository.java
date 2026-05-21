package com.PFE.backend.repositories;

import com.PFE.backend.models.Tool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolsRepository extends JpaRepository<Tool, String> {
}
