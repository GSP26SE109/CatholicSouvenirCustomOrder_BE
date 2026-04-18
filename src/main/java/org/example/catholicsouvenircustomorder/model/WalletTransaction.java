package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "wallet_transactions", indexes = {
    @Index(name = "idx_wallet_transactions_commission", columnList = "commission_fee, created_at")
})
public class WalletTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID transactionId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionType type;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;
    
    // Reference to payment source
    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;
    
    @ManyToOne
    @JoinColumn(name = "stage_payment_id")
    private StagePayment stagePayment;
    
    // Related entity for tracking (e.g., COMPLAINT for refunds)
    @Enumerated(EnumType.STRING)
    @Column(name = "related_entity_type")
    private RelatedEntityType relatedEntityType;
    
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Commission fee deducted from this transaction (VND)
    @Column(name = "commission_fee", precision = 15, scale = 2)
    private BigDecimal commissionFee = BigDecimal.ZERO;
    
    // Commission rate applied to this transaction (%)
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
