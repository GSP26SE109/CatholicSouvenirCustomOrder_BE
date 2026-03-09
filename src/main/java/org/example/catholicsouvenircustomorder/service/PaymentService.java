package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreatePaymentRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(UUID stageId, PaymentMethod method, UUID customerId);
    PaymentResponse handleZaloPayCallback(Map<String, String> callbackData);
    PaymentResponse handleVNPayCallback(Map<String, String> callbackData);
    PaymentResponse getPaymentByStageId(UUID stageId);
}
