package com.PFE.backend.repositories;

import com.PFE.backend.models.Secret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecretRepository extends JpaRepository<Secret, String> {
    Optional<Secret> findByUserIdAndName(String userId, String name);
}
