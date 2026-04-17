package org.example.catholicsouvenircustomorder.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Artisan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID artisanUuid;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "artisan_id")
    @JsonIgnore
    private Account account;
    
    private String artisanName;
    private String bio;
    private int experience_year;
    private String portfolioUrl;
    private String specialization;
    
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;
    
    private String rejectionReason;
    private LocalDateTime submittedDate;
    private LocalDateTime reviewedDate;
    
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    @JsonIgnore
    private Account reviewedBy;

    @OneToMany(mappedBy = "artisan",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "artisan")
    @JsonIgnore
    private List<CustomOrder> customOrders = new ArrayList<>();
    
    @OneToMany(mappedBy = "selectedArtisan")
    @JsonIgnore
    private List<CustomRequest> selectedRequests = new ArrayList<>();
}
