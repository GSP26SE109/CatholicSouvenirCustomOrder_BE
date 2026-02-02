package org.example.catholicsouvenircustomorder.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Artisan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int artisanId;
    private UUID artisanUuid;
    private String artisanName;
    private String bio;
    private int experience_year;

}
