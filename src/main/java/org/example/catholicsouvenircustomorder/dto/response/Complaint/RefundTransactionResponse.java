package org.example.catholicsouvenircustomorder.dto.response.Complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refund transaction response DTO
 * Requirements: 9.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTransactionResponse {
    private UUID refundTransactionId;
    private UUID complaintId;
    private BigDecimal amount;
    
    // Wallet info
    private UUID fromWalletId;
    private String fromWalletOwnerName;
    private UUID toWalletId;
    private String toWalletOwnerName;
    
    // Status
    private RefundStatus status;
    private String failureReason;
    
    // Transaction references
    private UUID debitTransactionId;
    private UUID creditTransactionId;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
