package org.example.catholicsouvenircustomorder.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "custom_order_stages")
public class CustomOrderStage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID stageId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private CustomOrder customOrder;
    
    @Column(nullable = false)
    private Integer stageOrder;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private Integer paymentPercentage;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    
    private String completionImageUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
