package com.PFE.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Sprint")
public class Sprint {
    @Id
    @GeneratedValue(strategy =  GenerationType.UUID)
    private String id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Enumerated(EnumType.STRING)
    private SprintStatus status;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @ManyToOne
    @JoinColumn(name = "scrum_master_id")
    @JsonIgnore
    private ScrumMaster scrumMaster;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Task> tasks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Sprint() {}

    public Sprint(String name, String goal, LocalDateTime endDate, ScrumMaster scrumMaster) {
        this.name = name;
        this.goal = goal;
        this.endDate = endDate;
        this.scrumMaster = scrumMaster;
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); status = SprintStatus.PLANNING; }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // getters & setters
    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public SprintStatus getStatus() { return status; }
    public void setStatus(SprintStatus status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public ScrumMaster getScrumMaster() { return scrumMaster; }
    public void setScrumMaster(ScrumMaster scrumMaster) { this.scrumMaster = scrumMaster; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
