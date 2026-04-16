package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID withdrawalId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "artisan_id")
    private Artisan artisan;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;
    
    // Bank account information
    @Column(nullable = false, length = 100)
    private String bankName;
    
    @Column(nullable = false, length = 50)
    private String bankAccountNumber;
    
    @Column(nullable = false, length = 255)
    private String bankAccountName;
    
    // Processing information
    @ManyToOne
    @JoinColumn(name = "processed_by")
    private Account processedBy; // Admin who approved/rejected
    
    private LocalDateTime processedAt;
    
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
    
    // Transaction reference
    @OneToOne
    @JoinColumn(name = "wallet_transaction_id")
    private WalletTransaction walletTransaction;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime cancelledAt;
    
    @Version
    private Long version; // For optimistic locking
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = WithdrawalStatus.PENDING;
        }
    }
}
