package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;

import java.util.List;
import java.util.UUID;

public interface CustomOrderStageService {
    CustomOrderStageResponse getStageById(UUID stageId, UUID userId);
    List<CustomOrderStageResponse> getStagesByOrderId(UUID orderId, UUID userId);
    CustomOrderStageResponse completeStage(UUID stageId, CompleteStageRequest request, UUID artisanId);
    CustomOrderStageResponse uploadProofImage(UUID stageId, String imageUrl, UUID artisanId);
    PaymentInitiationResponse initiateStagePayment(UUID stageId, InitiatePaymentDTO paymentRequest, UUID customerId);
    boolean canPayStage(UUID stageId);
}
