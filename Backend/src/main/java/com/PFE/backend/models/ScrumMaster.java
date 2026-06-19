package com.PFE.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ScrumMaster")
public class ScrumMaster extends User {
    @OneToMany(mappedBy = "scrumMaster", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Sprint> sprints;

    @OneToMany(mappedBy = "taskFrom", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Task> tasksAdded;

    @OneToMany(mappedBy = "scrumMaster", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<User> team;

    public ScrumMaster() { super(); }

    public List<Sprint> getSprints() { return sprints; }
    public void setSprints(List<Sprint> sprints) { this.sprints = sprints; }

    public List<Task> getTasksAdded() { return tasksAdded; }
    public void setTasksAdded(List<Task> tasksAdded) { this.tasksAdded = tasksAdded; }

    public List<User> getTeam() { return team; }
    public void setTeam(List<User> team) { this.team = team; }
}
