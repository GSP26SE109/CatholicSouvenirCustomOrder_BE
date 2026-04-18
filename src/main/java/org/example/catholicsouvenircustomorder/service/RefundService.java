package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.Complaint.RefundTransactionResponse;
import org.example.catholicsouvenircustomorder.model.Complaint;
import org.example.catholicsouvenircustomorder.model.RefundStatus;
import org.example.catholicsouvenircustomorder.model.RefundTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service interface for refund processing
 * Handles refund transactions for approved complaints
 */
public interface RefundService {
    
    /**
     * Process refund for approved complaint
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 11.4, 11.5
     */
    RefundTransaction processRefund(Complaint complaint, BigDecimal amount);
    
    /**
     * Retry failed refund (admin action)
     * Requirements: 11.5
     */
    RefundTransaction retryRefund(UUID refundTransactionId, UUID adminId);
    
    /**
     * Get refund transaction details
     * Requirements: 9.1
     */
    RefundTransactionResponse getRefundTransaction(UUID refundTransactionId);
    
    /**
     * Get all refund transactions with optional status filter
     * Requirements: 9.1
     */
    Page<RefundTransactionResponse> getAllRefundTransactions(RefundStatus status, Pageable pageable);
}
