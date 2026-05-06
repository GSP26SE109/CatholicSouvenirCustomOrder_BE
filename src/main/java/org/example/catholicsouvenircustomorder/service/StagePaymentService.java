package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.StagePaymentResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for StagePayment operations.
 * Handles stage-based payments for request-based custom orders.
 * A stage can have multiple payment attempts.
 */
public interface StagePaymentService {
    
    /**
     * Create payment attempt for a specific stage
     */
    StagePaymentResponse createStagePayment(UUID stageId, UUID customerId, String paymentMethod);
    
    /**
     * Handle payment callback for stage payment
     */
    StagePaymentResponse handleStagePaymentCallback(String transactionId, String status);
    
    /**
     * Handle payment callback for stage payment with transaction ID
     */
    StagePaymentResponse handleStagePaymentCallback(String referenceId, String status, String transactionId);
    
    /**
     * Get successful payment for a stage
     */
    StagePaymentResponse getSuccessfulStagePayment(UUID stageId);
    
    /**
     * Get all payment attempts for a stage (including failed ones)
     */
    List<StagePaymentResponse> getStagePaymentHistory(UUID stageId);
    
    /**
     * Check if stage payment is completed
     */
    boolean isStagePaymentCompleted(UUID stageId);
    
    /**
     * Get return URL from payment by reference ID
     */
    String getReturnUrlByReferenceId(String referenceId);
}
