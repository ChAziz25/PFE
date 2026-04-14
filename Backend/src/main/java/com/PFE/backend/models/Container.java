package com.PFE.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "containers")
public class Container {

    @Id
    private String id;
    private String name;

    private LocalDateTime createdAt;
    private LocalDateTime lastStartedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        lastStartedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        lastStartedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastStartedAt() { return lastStartedAt; }
}
