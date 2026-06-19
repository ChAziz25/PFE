package com.PFE.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String email;
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Secret> secrets;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Container> containers;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Tool> tools;

    @OneToMany(mappedBy = "taskFor", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Task> tasks;

    @ManyToOne
    @JoinColumn(name = "scrum_master_id")
    private ScrumMaster scrumMaster;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(){}

    public User(
            String name, String email, String password, List<Secret> secrets
    ) {
        this.name = name;
        this.email = email;
        this.password = password;
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

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<Secret> getSecrets() { return secrets; }
    public void setSecrets(List<Secret> secrets) { this.secrets = secrets; }

    public List<Container> getContainers() { return containers; }
    public void setContainers(List<Container> containers) { this.containers = containers; }

    public List<Tool> getTools(){ return tools; }
    public void setTools(List<Tool> tools) {this.tools = tools; }

    public ScrumMaster getScrumMaster() { return scrumMaster; }
    public void setScrumMaster(ScrumMaster scrumMaster) { this.scrumMaster = scrumMaster; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
