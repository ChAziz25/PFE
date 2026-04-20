package com.PFE.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Containers")
public class Container {

    @Id
    private String id;

    private String name;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime lastStartedAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    public Container(){
        this.status = "CREATED";
    }
    public Container(String id, String name){
        this.id = id;
        this.name = name;
        this.status = "CREATED";
    }

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        lastStartedAt = LocalDateTime.now();
        lastUsed = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status;}

    public void setLastStartedAt(LocalDateTime lastStartedAt) { this.lastStartedAt = lastStartedAt; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastStartedAt() { return lastStartedAt; }
    public LocalDateTime getLastUsed() { return lastUsed;}
}
