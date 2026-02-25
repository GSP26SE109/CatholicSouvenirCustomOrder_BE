package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "artisan_application")
public class ArtisanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID applicationId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String artisanName;
    
    @Column(length = 1000, nullable = false)
    private String bio;
    
    @Column(nullable = false)
    private int experienceYear;
    
    private String portfolioUrl;
    
    @Column(nullable = false)
    private String specialization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;
    
    @Column(nullable = false)
    private LocalDateTime submittedDate;
    
    private LocalDateTime reviewedDate;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private Account reviewedBy;
}
