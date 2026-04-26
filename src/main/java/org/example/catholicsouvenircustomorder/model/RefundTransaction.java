package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "refund_transactions")
public class RefundTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID refundTransactionId;
    
    @OneToOne(optional = false)
    @JoinColumn(name = "complaint_id", unique = true)
    private Complaint complaint;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet; // Artisan wallet
    
    // VNPay refund fields
    @Column(length = 100)
    private String vnpayRefundId;
    
    @Column(length = 100)
    private String vnpayTransactionNo;
    
    @Column
    private UUID originalPaymentId; // Reference to original Payment entity
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status = RefundStatus.PENDING;
    
    @Column(length = 500)
    private String failureReason;
    
    // Reference to WalletTransaction entry (debit from Artisan only)
    @OneToOne
    @JoinColumn(name = "debit_transaction_id")
    private WalletTransaction debitTransaction;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime completedAt;
    
    @PreUpdate
    public void preUpdate() {
        if (status == RefundStatus.COMPLETED || status == RefundStatus.FAILED || status == RefundStatus.PARTIALLY_REFUNDED) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
