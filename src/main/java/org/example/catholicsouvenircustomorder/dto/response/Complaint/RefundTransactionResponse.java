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
 * Updated to include VNPay refund fields
 * Requirements: 8.1, 8.2, 9.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTransactionResponse {
    private UUID refundTransactionId;
    private UUID complaintId;
    private BigDecimal amount;
    
    // Artisan wallet info (from wallet)
    private UUID fromWalletId;
    private String fromWalletOwnerName;
    
    // VNPay refund info (replaces toWallet)
    private String vnpayRefundId;
    private String vnpayTransactionNo;
    private UUID originalPaymentId;
    
    // Status
    private RefundStatus status;
    private String failureReason;
    
    // Transaction reference (debit only, credit removed)
    private UUID debitTransactionId;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
