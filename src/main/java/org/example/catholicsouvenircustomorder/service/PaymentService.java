package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.PaymentCallbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.model.CustomOrder;
import org.example.catholicsouvenircustomorder.model.CustomOrderStage;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    
    // New unified payment methods
    PaymentInitiationResponse initiatePayment(InitiatePaymentDTO dto);
    
    PaymentResponse handlePaymentCallback(PaymentCallbackRequest request);
    
    List<PaymentResponse> getOrderPayments(UUID orderId);
    
    List<PaymentResponse> getCustomOrderPayments(UUID customOrderId);
    
    List<PaymentResponse> getStagePayments(UUID stageId);
    
    boolean isOrderFullyPaid(UUID orderId);
    
    boolean isCustomOrderFullyPaid(UUID customOrderId);
    
    boolean isStageFullyPaid(UUID stageId);
    
    PaymentResponse refundPayment(UUID paymentId, String reason);
    
    Payment findById(UUID paymentId);
}
