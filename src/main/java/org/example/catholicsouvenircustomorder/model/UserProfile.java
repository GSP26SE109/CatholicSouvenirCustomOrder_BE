package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID profileId;
    
    @OneToOne
    @JoinColumn(name = "account_id", unique = true, nullable = false)
    private Account account;
    
    // Basic Information (synced from Account)
    @Column(length = 255)
    private String fullName;
    
    @Column(length = 255)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 10)
    private String gender;
    
    private String dateOfBirth;
    
    @Column(length = 500)
    private String avatarUrl;
    
    // Additional Profile Information
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Column(length = 255)
    private String address;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String district;
    
    @Column(length = 100)
    private String ward;
    
    @Column(length = 20)
    private String postalCode;
    
    // Saint Information (simplified - no entity relationship)
    @Column(length = 100)
    private String saintName;
    
    // Preferences
    @Column(length = 10)
    private String language = "vi"; // Default Vietnamese
    
    @Column(length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";
    
    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
