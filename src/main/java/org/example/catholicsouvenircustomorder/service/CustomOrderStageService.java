package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.InitiateStagePaymentRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;

import java.util.List;
import java.util.UUID;

public interface CustomOrderStageService {
    /**
     * Get stage by ID (with ownership check)
     */
    CustomOrderStageResponse getStageById(UUID stageId, UUID userId);
    
    /**
     * Artisan starts working on a stage
     * Changes status from PAID to IN_PROGRESS
     */
    CustomOrderStageResponse startStage(UUID stageId, UUID artisanId);
    
    /**
     * Artisan completes a stage
     * This will unlock the next stage for payment
     */
    CustomOrderStageResponse completeStage(UUID stageId, CompleteStageRequest request, UUID artisanId);
    
    /**
     * Artisan uploads proof image for a stage
     */
    CustomOrderStageResponse uploadProofImage(UUID stageId, String imageUrl, UUID artisanId);
    
    /**
     * Check if a stage can be paid
     * Uses workflow flags (canPay && !isPaid)
     */
    boolean canPayStage(UUID stageId);
    
    /**
     * Initiate payment for a stage
     * NOTE: This is called from StagePaymentController but implemented here
     * because it needs access to stage validation logic
     */
    PaymentInitiationResponse initiateStagePayment(UUID stageId, InitiateStagePaymentRequest paymentRequest, UUID customerId);
}
