package com.PFE.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Tools")
public class Tools {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(columnDefinition = "TEXT")
    private String script;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToMany
    private List<Container> containers;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Tools(){}

    public Tools(String script, User user){
        this.script = script;
        this.user = user;
    }

    public Tools(String script, User user, List<Container> container){
        this.script = script;
        this.user = user;
        this.containers = container;
    }

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
    }

    public String getId(){ return id; }

    public void setScript(String script){ this.script = script; }
    public String getScript(){ return script; }

    public void setUser(User user){ this.user = user; }
    public User getUser(){ return user; }

    public void setContainers(List<Container> containers){ this.containers = containers; }
    public List<Container> getContainers(){ return containers; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
