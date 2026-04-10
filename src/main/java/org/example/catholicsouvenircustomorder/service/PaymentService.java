package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.PaymentCallbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.model.Payment;

import java.util.List;
import java.util.UUID;

/**
 * Service for handling Order payments (template/product orders)
 * For custom order stage payments, use StagePaymentService
 */
public interface PaymentService {
    
    /**
     * Khởi tạo payment cho Order
     */
    PaymentInitiationResponse initiatePayment(InitiatePaymentDTO dto);
    
    /**
     * Xử lý callback từ payment gateway
     */
    PaymentResponse handlePaymentCallback(PaymentCallbackRequest request);
    
    /**
     * Lấy danh sách payments của một order
     */
    List<PaymentResponse> getOrderPayments(UUID orderId);
    
    /**
     * Lấy payment theo ID
     */
    PaymentResponse getPaymentById(UUID paymentId);
    
    /**
     * Kiểm tra xem order đã được thanh toán đầy đủ chưa
     */
    boolean isOrderFullyPaid(UUID orderId);
    
    /**
     * Hoàn tiền
     */
    PaymentResponse refundPayment(UUID paymentId, String reason);
    
    /**
     * Tìm payment entity theo ID
     */
    Payment findById(UUID paymentId);
}
