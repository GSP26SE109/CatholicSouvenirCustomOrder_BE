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
    
    @OneToOne
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;
    
    @ManyToOne
    @JoinColumn(name = "custom_order_id")
    private CustomOrder customOrder;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_source")
    private RefundSource refundSource;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by")
    private CancellationInitiator cancelledBy;

    
    @Column(name = "refund_calculation_details", columnDefinition = "TEXT")
    private String calculationDetails;
    
    @Column(name = "platform_commission_amount", precision = 18, scale = 2)
    private BigDecimal platformCommissionAmount = BigDecimal.ZERO;
    
    @Column(name = "net_refund_amount", precision = 18, scale = 2)
    private BigDecimal netRefundAmount = BigDecimal.ZERO;
    
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
