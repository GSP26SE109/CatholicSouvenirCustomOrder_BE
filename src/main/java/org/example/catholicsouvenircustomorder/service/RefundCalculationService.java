package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.CancellationEstimate;
import org.example.catholicsouvenircustomorder.dto.response.StageRefundCalculation;
import org.example.catholicsouvenircustomorder.model.CancellationInitiator;
import org.example.catholicsouvenircustomorder.model.CustomOrder;

import java.util.List;
import java.util.UUID;

/**
 * Service for calculating refund amounts with platform commission
 */
public interface RefundCalculationService {
    
    /**
     * Calculate refund for each stage with platform commission
     * @param order The custom order to calculate refunds for
     * @param initiator Who initiated the cancellation (CUSTOMER or ARTISAN)
     * @return List of stage refund calculations
     */
    List<StageRefundCalculation> calculateStageRefunds(CustomOrder order, CancellationInitiator initiator);
    
    /**
     * Calculate cancellation estimate without processing actual refund
     * @param orderId The order ID to calculate estimate for
     * @param initiator Who is initiating the cancellation
     * @return Cancellation estimate with balance check
     */
    CancellationEstimate calculateRefundEstimate(UUID orderId, CancellationInitiator initiator);
}
