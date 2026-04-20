package com.PFE.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commands")
public class Command {

    @Id
    private String id;

    private String containerId;
    private String command;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String output;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
    }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
