package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.CancellationEstimate;
import org.example.catholicsouvenircustomorder.model.CancellationInitiator;
import org.example.catholicsouvenircustomorder.model.RefundTransaction;

import java.util.UUID;

/**
 * Service for handling order cancellation and refund processing
 * Requirements: 10.1, 10.2
 */
public interface CancellationService {
    
    /**
     * Cancel order and process refund
     * @param customOrderId The order to cancel
     * @param initiatedBy The account ID initiating the cancellation
     * @param initiator Who is cancelling (CUSTOMER or ARTISAN)
     * @param reason Cancellation reason
     * @return RefundTransaction record
     */
    RefundTransaction cancelOrder(
        UUID customOrderId,
        UUID initiatedBy,
        CancellationInitiator initiator,
        String reason
    );
    
    /**
     * Calculate refund estimate before cancellation
     * @param customOrderId The order ID to calculate estimate for
     * @param initiator Who is initiating the cancellation
     * @return Cancellation estimate with balance check
     */
    CancellationEstimate calculateRefundEstimate(UUID customOrderId, CancellationInitiator initiator);
    
    /**
     * Check if order can be cancelled
     * @param customOrderId The order ID to check
     * @param initiator Who is initiating the cancellation
     * @return true if order can be cancelled
     */
    boolean canCancelOrder(UUID customOrderId, CancellationInitiator initiator);
}
