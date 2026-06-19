package com.PFE.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    @ManyToOne
    @JoinColumn(name = "sprint_id")
    @JsonIgnore
    private Sprint sprint;

    @ManyToOne
    @JoinColumn(name = "task_for_user_id")
    @JsonIgnore
    private User taskFor;

    @ManyToOne
    @JoinColumn(name = "task_from_scrum_master_id")
    @JsonIgnore
    private ScrumMaster taskFrom;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task(){}

    public Task(String name, String description, TaskPriority priority, Sprint sprint, User taskFor, ScrumMaster taskFrom) {
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.sprint = sprint;
        this.taskFor = taskFor;
        this.taskFrom = taskFrom;
    }

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        status = TaskStatus.TODO;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public Sprint getSprint() { return sprint; }
    public void setSprint(Sprint sprint) { this.sprint = sprint; }

    public User getTaskFor() { return taskFor; }
    public void setTaskFor(User taskFor) { this.taskFor = taskFor; }

    public ScrumMaster getTaskFrom() { return taskFrom; }
    public void setTaskFrom(ScrumMaster taskFrom) { this.taskFrom = taskFrom; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
