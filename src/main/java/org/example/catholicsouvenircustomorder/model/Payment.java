package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "payments")
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "custom_order_id")
    private CustomOrder customOrder;
    
    @ManyToOne
    @JoinColumn(name = "stage_id")
    private CustomOrderStage stage;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;
    
    @Column(unique = true, length = 100)
    private String transactionId;
    
    @Column(length = 1000)
    private String paymentUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime paidAt;
    
    @Column(columnDefinition = "TEXT")
    private String failureReason;
    
    @PreUpdate
    public void preUpdate() {
        if (this.status == PaymentStatus.SUCCESS && this.paidAt == null) {
            this.paidAt = LocalDateTime.now();
        }
    }
}
