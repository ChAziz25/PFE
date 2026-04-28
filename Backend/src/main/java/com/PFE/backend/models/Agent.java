package com.PFE.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Agent")
public class Agent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany
    private List<Secret> secrets;

    private LocalDateTime createdAt;

    public Agent(String id, String name, User user, List<Secret> secrets) {
        this.name = name;
    }

    @PrePersist
    protected void onCreate(){ createdAt = LocalDateTime.now(); }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Secret> getSecrets() { return secrets; }
    public void setSecrets(List<Secret> secrets) { this.secrets = secrets; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
