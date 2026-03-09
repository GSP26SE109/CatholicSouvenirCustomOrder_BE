package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.model.Payment;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PaymentService {
    String createPaymentByPayOS(UUID orderId, UUID stageId);

    Payment findById(UUID paymentId);
    PaymentResponse createPayment(UUID stageId, PaymentMethod method, UUID customerId);
    PaymentResponse handleZaloPayCallback(Map<String, String> callbackData);
    PaymentResponse handleVNPayCallback(Map<String, String> callbackData);
    PaymentResponse getPaymentByStageId(UUID stageId);
}
