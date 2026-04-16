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
     * VNPay Return URL endpoint - User is redirected here after payment
     * DO NOT update DB here - only redirect to frontend
     * DB update happens via IPN callback
     */
    @GetMapping("/vnpay/return")
    public void handleVNPayReturn(
            @RequestParam Map<String, String> params,
            @RequestParam(required = false) String platform,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        
        log.info("Received VNPay return callback");
        log.info("Platform: {}", platform);
        log.info("Response code: {}", params.get("vnp_ResponseCode"));
        
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        boolean isSuccess = "00".equals(vnpResponseCode);
        
        // Build redirect URL based on platform
        String redirectUrl;
        if ("mobile".equalsIgnoreCase(platform)) {
            // Mobile deep link
            redirectUrl = String.format("myapp://payment/result?orderId=%s&success=%s&code=%s",
                    txnRef, isSuccess, vnpResponseCode);
        } else {
            // Web frontend URL
            String frontendUrl = "https://your-frontend.com"; // TODO: Move to config
            redirectUrl = String.format("%s/payment/result?orderId=%s&success=%s&code=%s",
                    frontendUrl, txnRef, isSuccess, vnpResponseCode);
        }
        
        log.info("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
    
    /**
     * VNPay IPN endpoint (Server-to-Server callback)
     * VNPay will call this via GET to notify payment result
     * This is the AUTHORITATIVE source - UPDATE DB HERE
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> handleVNPayIPN(
            @RequestParam Map<String, String> params) {
        
        log.info("Received VNPay IPN notification");
        
        try {
            // IMPORTANT: Pass a copy of params to avoid modification issues
            PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                    .params(new HashMap<>(params))
                    .paymentGateway("VNPAY")
                    .build();
            
            paymentService.handlePaymentCallback(request);
            
            // VNPay expects specific response format (NO BaseResponse wrapper)
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
     * Lấy trạng thái payment mới nhất của order (cho mobile app polling)
     * Also returns payment status for checking if order is paid
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
