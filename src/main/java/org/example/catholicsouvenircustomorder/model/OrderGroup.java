package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OrderGroup represents a single checkout session that may contain multiple orders
 * from different artisans. Customer pays once for the entire group.
 */
@Entity
@Data
@Table(name = "order_groups")
public class OrderGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Account customer;
    
    @OneToMany(mappedBy = "orderGroup", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonManagedReference("orderGroup-orders")
    private List<Order> orders = new ArrayList<>();
    
    @OneToMany(mappedBy = "orderGroup", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<Payment> payments = new ArrayList<>();
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, PAID, CANCELLED
    
    @Column(nullable = false)
    private String paymentMethod;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
