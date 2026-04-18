package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SystemConfig entity - Stores system-wide configuration settings
 * Used for commission rate and other configurable parameters
 */
@Entity
@Data
@Table(name = "system_config")
public class SystemConfig {
    
    @Id
    @Column(name = "config_key", length = 255)
    private String configKey;
    
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "updated_by")
    private Account updatedBy;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
}
