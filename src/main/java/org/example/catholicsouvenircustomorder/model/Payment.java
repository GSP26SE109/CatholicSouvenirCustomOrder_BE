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
    
    // Order group payment - always use this for checkout payments
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_group_id", nullable = false)
    private OrderGroup orderGroup;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;
    
    // Reference ID for internal tracking (used to create payment URL)
    @Column(unique = true, length = 100, nullable = false)
    private String referenceId;
    
    // Transaction ID from payment gateway (VNPay/ZaloPay returns this in callback)
    @Column(unique = true, length = 100)
    private String transactionId;
    
    @Column(length = 1000)
    private String paymentUrl;
    
    // Return URL for redirecting user after payment (web or mobile deep link)
    @Column(length = 500)
    private String returnUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime paidAt;
    
    @Column(columnDefinition = "TEXT")
    private String failureReason;
    
    // Transaction relationship (1:1)
    
    @PreUpdate
    public void preUpdate() {
        if (this.status == PaymentStatus.SUCCESS && this.paidAt == null) {
            this.paidAt = LocalDateTime.now();
        }
    }
}
