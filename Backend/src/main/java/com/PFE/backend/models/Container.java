package com.PFE.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Containers")
public class Container {

    @Id
    private String id;

    private String name;
    private String status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User owner;

    @ManyToMany(mappedBy = "containers")
    private List<Tool> tools;

    private LocalDateTime createdAt;
    private LocalDateTime lastStartedAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    public Container(){
        this.status = "CREATED";
    }
    public Container(String id, String name, User owner){
        this.id = id;
        this.name = name;
        this.owner = owner;
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

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<Tool> getTools() { return tools; }
    public void setTools(List<Tool> tools) { this.tools = tools; }

    public void setLastStartedAt(LocalDateTime lastStartedAt) { this.lastStartedAt = lastStartedAt; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastStartedAt() { return lastStartedAt; }
    public LocalDateTime getLastUsed() { return lastUsed;}
}
