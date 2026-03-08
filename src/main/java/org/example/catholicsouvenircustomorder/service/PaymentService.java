package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.Payment;

import java.util.UUID;

public interface PaymentService {
    String createPaymentByPayOS(UUID orderId, UUID stageId);

    Payment findById(UUID paymentId);
}
