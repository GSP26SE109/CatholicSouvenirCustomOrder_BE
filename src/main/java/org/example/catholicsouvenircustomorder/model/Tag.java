package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import jakarta.persistence.GenerationType;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tagId;

    @Column(unique = true, nullable = false)
    private String name;
}