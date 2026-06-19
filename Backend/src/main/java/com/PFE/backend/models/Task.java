package com.PFE.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "Task")
public class Tasks {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    
}
