package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Category entity for organizing products and templates.
 * Simple flat structure without hierarchy.
 */
@Entity
@Data
@Table(name = "categories")
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID categoryId;
    
    @Column(nullable = false, unique = true, length = 100)
    private String categoryName;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private Integer sortOrder = 0;
    
    // Icon/Image for category display
    @Column(length = 500)
    private String iconUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
