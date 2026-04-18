package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * StagePayment entity - Payment attempts for CustomOrderStage
 * A stage can have multiple payment attempts (retry on failure, different methods, etc.)
 */
@Entity
@Data
@Table(name = "stage_payments")
public class StagePayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;
    
    @ManyToOne(optional = false)
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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;
    
    // Reference ID for internal tracking (used to create payment URL)
    @Column(unique = true, length = 100, nullable = false)
    private String referenceId;
    
    // Transaction ID from payment gateway (VNPay/ZaloPay returns this in callback)
    @Column(unique = true, length = 100)
    private String transactionId;
    
    @Column(length = 1000)
    private String paymentUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime paidAt;
    
    @Column(columnDefinition = "TEXT")
    private String failureReason;
    
    // Frontend return URL (where to redirect after payment)
    @Column(length = 500)
    private String returnUrl;
    
    // Commission rate snapshot at stage payment creation time (%)
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;
    
    // Transaction relationship (1:1)
    
    @PreUpdate
    public void preUpdate() {
        if (this.status == PaymentStatus.SUCCESS && this.paidAt == null) {
            this.paidAt = LocalDateTime.now();
        }
    }
}
