package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "feedbacks")
public class Feedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID feedbackId;
    
    // Relationship to Order (nullable for CustomOrder)
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    // Relationship to CustomOrder (nullable for Order)
    @ManyToOne
    @JoinColumn(name = "custom_order_id")
    private CustomOrder customOrder;
    
    // Customer who created the feedback
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Account customer;
    
    // Artisan who receives the feedback
    @ManyToOne(optional = false)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;
    
    // Rating (1-5 stars)
    @Column(nullable = false)
    private Integer rating;
    
    // Optional comment
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    // Timestamp
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
