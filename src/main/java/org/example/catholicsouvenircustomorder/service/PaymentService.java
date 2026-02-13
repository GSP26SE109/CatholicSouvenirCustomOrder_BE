package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentService {
    String createPayment(UUID orderId, UUID stageId);

    Payment findById(UUID paymentId);
}
