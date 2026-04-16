package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.PaymentCallbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.PaymentRepository;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    

    
    /**
     * Khởi tạo payment cho Order (template/product order)
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<PaymentInitiationResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentDTO dto,
            @AuthenticationPrincipal UUID accountId) {
        
        log.info("Initiating payment for order: {}", dto.getOrderId());
        
        // Validate order exists and belongs to customer
        if (dto.getOrderId() == null) {
            throw new BadRequestException("Order ID không được để trống");
        }
        
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
        
        if (!order.getCustomer().getAccountId().equals(accountId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán đơn hàng này");
        }
        
        PaymentInitiationResponse response = paymentService.initiatePayment(dto);
        
        return ResponseEntity.ok(BaseResponse.<PaymentInitiationResponse>builder()
                .code(200)
                .message("Khởi tạo thanh toán thành công")
                .data(response)
                .build());
    }
    
    /**
     * Xử lý callback từ payment gateway (VNPay, ZaloPay)
     */
    @PostMapping("/callback")
    public ResponseEntity<BaseResponse<PaymentResponse>> handlePaymentCallback(
            @RequestBody Map<String, String> params,
            @RequestParam(required = false, defaultValue = "VNPAY") String gateway) {
        
        log.info("Received payment callback from gateway: {}", gateway);
        
        PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                .params(params)
                .paymentGateway(gateway)
                .build();
        
        PaymentResponse response = paymentService.handlePaymentCallback(request);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Xử lý callback thanh toán thành công")
                .data(response)
                .build());
    }
    
    /**
     * VNPay callback endpoint (GET method)
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<BaseResponse<PaymentResponse>> handleVNPayCallback(
            @RequestParam Map<String, String> params) {
        
        log.info("Received VNPay callback");
        
        PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                .params(params)
                .paymentGateway("VNPAY")
                .build();
        
        PaymentResponse response = paymentService.handlePaymentCallback(request);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Xử lý callback VNPay thành công")
                .data(response)
                .build());
    }
    
    /**
     * VNPay IPN endpoint (Server-to-Server callback)
     * VNPay will POST here to notify payment result
     * This is the AUTHORITATIVE source of payment status
     */
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> handleVNPayIPN(
            @RequestParam Map<String, String> params) {
        
        log.info("Received VNPay IPN notification");
        
        try {
            PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                    .params(params)
                    .paymentGateway("VNPAY")
                    .build();
            
            paymentService.handlePaymentCallback(request);
            
            // VNPay expects specific response format
            Map<String, String> response = new HashMap<>();
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing VNPay IPN", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * ZaloPay callback endpoint (POST method)
     */
    @PostMapping("/zalopay/callback")
    public ResponseEntity<BaseResponse<PaymentResponse>> handleZaloPayCallback(
            @RequestBody Map<String, String> params) {
        
        log.info("Received ZaloPay callback");
        
        PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                .params(params)
                .paymentGateway("ZALOPAY")
                .build();
        
        PaymentResponse response = paymentService.handlePaymentCallback(request);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Xử lý callback ZaloPay thành công")
                .data(response)
                .build());
    }
    
    /**
     * ZaloPay IPN endpoint (Server-to-Server callback)
     * ZaloPay will POST here to notify payment result
     * This is the AUTHORITATIVE source of payment status
     */
    @PostMapping("/zalopay/ipn")
    public ResponseEntity<Map<String, Object>> handleZaloPayIPN(
            @RequestBody Map<String, String> params) {
        
        log.info("Received ZaloPay IPN notification");
        
        try {
            PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                    .params(params)
                    .paymentGateway("ZALOPAY")
                    .build();
            
            paymentService.handlePaymentCallback(request);
            
            // ZaloPay expects specific response format
            Map<String, Object> response = new HashMap<>();
            response.put("return_code", 1);
            response.put("return_message", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing ZaloPay IPN", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("return_code", 0);
            response.put("return_message", "failed");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Lấy tất cả payments của user (customer hoặc admin)
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<PaymentResponse>>> getUserPayments(
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        log.info("Getting payments for user: {}", accountId);
        
        List<PaymentResponse> payments;
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        if ("CUSTOMER".equals(role)) {
            // Get all payments for customer's orders
            List<Order> orders = orderRepository.findByCustomer_AccountId(accountId);
            payments = orders.stream()
                    .flatMap(order -> paymentService.getOrderPayments(order.getOrderId()).stream())
                    .collect(Collectors.toList());
            
        } else if ("ADMIN".equals(role)) {
            // Admin can see all payments
            payments = paymentRepository.findAll().stream()
                    .map(payment -> {
                        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                                .paymentId(payment.getPaymentId())
                                .paymentMethod(payment.getMethod())
                                .amount(payment.getAmount())
                                .paymentStatus(payment.getStatus())
                                .transactionId(payment.getTransactionId())
                                .paymentUrl(payment.getPaymentUrl())
                                .failureReason(payment.getFailureReason())
                                .createdAt(payment.getCreatedAt())
                                .paidAt(payment.getPaidAt());
                        
                        if (payment.getOrder() != null) {
                            builder.orderId(payment.getOrder().getOrderId());
                        }
                        
                        return builder.build();
                    })
                    .collect(Collectors.toList());
        } else {
            payments = List.of();
        }
        
        return ResponseEntity.ok(BaseResponse.<List<PaymentResponse>>builder()
                .code(200)
                .message("Lấy danh sách thanh toán thành công")
                .data(payments)
                .build());
    }
    
    /**
     * Lấy payments của một order cụ thể
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<PaymentResponse>>> getOrderPayments(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        log.info("Getting payments for order: {}", orderId);
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            if (!order.getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán của đơn hàng này");
            }
        }
        
        List<PaymentResponse> payments = paymentService.getOrderPayments(orderId);
        
        return ResponseEntity.ok(BaseResponse.<List<PaymentResponse>>builder()
                .code(200)
                .message("Lấy danh sách thanh toán thành công")
                .data(payments)
                .build());
    }
    
    /**
     * Lấy payment theo ID
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<BaseResponse<PaymentResponse>> getPaymentById(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        log.info("Getting payment: {}", paymentId);
        
        PaymentResponse payment = paymentService.getPaymentById(paymentId);
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            if (!order.getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán này");
            }
        }
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Lấy thông tin thanh toán thành công")
                .data(payment)
                .build());
    }
    
    /**
     * Kiểm tra trạng thái thanh toán của order
     */
    @GetMapping("/order/{orderId}/status")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<BaseResponse<Boolean>> checkOrderPaymentStatus(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        log.info("Checking payment status for order: {}", orderId);
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            if (!order.getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem trạng thái thanh toán của đơn hàng này");
            }
        }
        
        boolean isPaid = paymentRepository.isOrderFullyPaid(orderId);
        
        return ResponseEntity.ok(BaseResponse.<Boolean>builder()
                .code(200)
                .message(isPaid ? "Đơn hàng đã được thanh toán" : "Đơn hàng chưa được thanh toán")
                .data(isPaid)
                .build());
    }
    
    /**
     * Lấy trạng thái payment mới nhất của order (cho mobile app polling)
     */
    @GetMapping("/order/{orderId}/latest")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<BaseResponse<PaymentResponse>> getLatestOrderPayment(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        log.info("Getting latest payment for order: {}", orderId);
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            if (!order.getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán của đơn hàng này");
            }
        }
        
        List<PaymentResponse> payments = paymentService.getOrderPayments(orderId);
        
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy thanh toán cho đơn hàng này");
        }
        
        // Return the most recent payment
        PaymentResponse latestPayment = payments.get(0);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Lấy trạng thái thanh toán thành công")
                .data(latestPayment)
                .build());
    }
    
    /**
     * Hoàn tiền (chỉ admin)
     */
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<PaymentResponse>> refundPayment(
            @PathVariable UUID paymentId,
            @RequestParam String reason) {
        
        log.info("Refunding payment: {}", paymentId);
        
        PaymentResponse response = paymentService.refundPayment(paymentId, reason);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Hoàn tiền thành công")
                .data(response)
                .build());
    }
}
