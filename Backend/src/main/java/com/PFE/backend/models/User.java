package com.PFE.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String email;
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Agent> agents;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Secret> secrets;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Container> containers;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(){}

    public User(
            String name, String email, String password, List<Agent> agents, List<Secret> secrets
    ) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.agents = agents;
        this.secrets = secrets;
    }

    public User (String name, String email, String password){
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<Agent> getAgents() { return agents; }
    public void setAgents(List<Agent> agents) { this.agents = agents; }

    public List<Secret> getSecrets() { return secrets; }
    public void setSecrets(List<Secret> secrets) { this.secrets = secrets; }

    public List<Container> getContainers() { return containers; }
    public void setContainers(List<Container> containers) { this.containers = containers; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
