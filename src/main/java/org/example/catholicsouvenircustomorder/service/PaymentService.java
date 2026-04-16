package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.PaymentCallbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.model.Payment;

import java.util.List;
import java.util.UUID;

/**
 * Service for handling OrderGroup payments (checkout payments)
 * For custom order stage payments, use StagePaymentService
 */
public interface PaymentService {
    
    /**
     * Khởi tạo payment cho OrderGroup (checkout)
     * Includes validation of orderGroup ownership
     */
    PaymentInitiationResponse initiatePayment(InitiatePaymentDTO dto, UUID customerId);
    
    /**
     * Xử lý callback từ payment gateway
     * Updates all orders in the order group when payment succeeds
     */
    PaymentResponse handlePaymentCallback(PaymentCallbackRequest request);
    
    /**
     * Lấy danh sách payments của một order group
     * Includes ownership validation for customers
     */
    List<PaymentResponse> getOrderGroupPayments(UUID orderGroupId, UUID customerId, String role);
    
    /**
     * Lấy payment theo ID
     * Includes ownership validation for customers
     */
    PaymentResponse getPaymentById(UUID paymentId, UUID customerId, String role);
    
    /**
     * Lấy danh sách payments của user (customer hoặc admin)
     */
    List<PaymentResponse> getUserPayments(UUID customerId, String role);
    
    /**
     * Kiểm tra xem order group đã được thanh toán đầy đủ chưa
     */
    boolean isOrderGroupPaid(UUID orderGroupId);
    
    /**
     * Hoàn tiền
     */
    PaymentResponse refundPayment(UUID paymentId, String reason);
    
    /**
     * Tìm payment entity theo ID
     */
    Payment findById(UUID paymentId);
}
