package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.PaymentCallbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.repository.PaymentRepository;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    
    @Value("${app.frontend-url}")
    private String defaultFrontendUrl;
    
    /**
     * Khởi tạo payment cho OrderGroup (checkout)
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<PaymentInitiationResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentDTO dto,
            @AuthenticationPrincipal UUID accountId) {
        
        log.info("Initiating payment for order group: {} by customer: {}", dto.getOrderGroupId(), accountId);
        
        PaymentInitiationResponse response = paymentService.initiatePayment(dto, accountId);
        
        return ResponseEntity.ok(BaseResponse.<PaymentInitiationResponse>builder()
                .code(200)
                .message("Khởi tạo thanh toán thành công")
                .data(response)
                .build());
    }
    
    /**
     * VNPay Return URL endpoint - User is redirected here after payment
     * IMPORTANT: Update DB here as backup in case IPN fails or delays
     */
    @GetMapping("/vnpay/return")
    public void handleVNPayReturn(
            @RequestParam Map<String, String> params,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        
        log.info("========================================");
        log.info("Received VNPay return callback");
        log.info("All params: {}", params);
        log.info("========================================");
        
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        boolean isSuccess = "00".equals(vnpResponseCode);
        
        log.info("Response code: {}, TxnRef: {}, Success: {}", vnpResponseCode, txnRef, isSuccess);
        
        // CRITICAL: Update DB here (backup for IPN)
        // IPN may fail or delay, so we update DB immediately when user returns
        try {
            log.info("Updating payment status via return URL...");
            
            // Filter only VNPay params (vnp_*)
            Map<String, String> vnpParams = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("vnp_")) {
                    vnpParams.put(entry.getKey(), entry.getValue());
                }
            }
            
            PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                    .params(vnpParams)
                    .paymentGateway("VNPAY")
                    .build();

            
        } catch (Exception e) {
            log.error("Error updating payment via return URL", e);
            // Continue to redirect even if update fails
        }
        
        // Redirect user to frontend
        String redirectUrl;
        try {
            var payment = paymentRepository.findByReferenceId(txnRef);
            
            if (payment.isPresent() && payment.get().getReturnUrl() != null && !payment.get().getReturnUrl().isEmpty()) {
                String customReturnUrl = payment.get().getReturnUrl();
                String separator = customReturnUrl.contains("?") ? "&" : "?";
                redirectUrl = String.format("%s%ssuccess=%s&code=%s&txnRef=%s",
                        customReturnUrl, separator, isSuccess, vnpResponseCode, txnRef);
            } else {
                redirectUrl = String.format("%s/payment/result?success=%s&code=%s&txnRef=%s",
                        defaultFrontendUrl, isSuccess, vnpResponseCode, txnRef);
            }
        } catch (Exception e) {
            log.error("Error getting return URL", e);
            redirectUrl = String.format("%s/payment/result?success=%s&code=%s&txnRef=%s",
                    defaultFrontendUrl, isSuccess, vnpResponseCode, txnRef);
        }
        
        log.info("Redirecting to: {}", redirectUrl);
        log.info("DB will be updated by IPN callback");
        log.info("========================================");
        response.sendRedirect(redirectUrl);
    }
    
    /**
     * VNPay IPN endpoint (Server-to-Server callback)
     * VNPay will call this via GET to notify payment result
     * This is the AUTHORITATIVE source - UPDATE DB HERE
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> handleVNPayIPN(
            @RequestParam Map<String, String> allParams) {
        
        log.info("========================================");
        log.info("Received VNPay IPN notification");
        log.info("All IPN params: {}", allParams);
        log.info("========================================");
        
        try {
            // Filter only VNPay parameters (vnp_*)
            // Remove any extra parameters like 'platform' that we added
            Map<String, String> vnpParams = new HashMap<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("vnp_")) {
                    vnpParams.put(entry.getKey(), entry.getValue());
                }
            }
            
            log.info("Filtered VNPay params: {}", vnpParams);
            
            // IMPORTANT: Pass only VNPay params for signature verification
            PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                    .params(vnpParams)
                    .paymentGateway("VNPAY")
                    .build();
            
            paymentService.handlePaymentCallback(request);
            
            log.info("IPN processing completed successfully");
            
            // VNPay expects specific response format (NO BaseResponse wrapper)
            Map<String, String> response = new HashMap<>();
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("Error processing VNPay IPN", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("========================================");
            
            Map<String, String> response = new HashMap<>();
            response.put("RspCode", "99");
            response.put("Message", e.getMessage() != null ? e.getMessage() : "Unknown error");
            
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
        
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        List<PaymentResponse> payments = paymentService.getUserPayments(accountId, role);
        
        return ResponseEntity.ok(BaseResponse.<List<PaymentResponse>>builder()
                .code(200)
                .message("Lấy danh sách thanh toán thành công")
                .data(payments)
                .build());
    }
    
    /**
     * Lấy payments của một order group cụ thể
     */
    @GetMapping("/order-group/{orderGroupId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<PaymentResponse>>> getOrderGroupPayments(
            @PathVariable UUID orderGroupId,
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        log.info("Getting payments for order group: {} by user: {}", orderGroupId, accountId);
        
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        List<PaymentResponse> payments = paymentService.getOrderGroupPayments(orderGroupId, accountId, role);
        
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
        
        log.info("Getting payment: {} by user: {}", paymentId, accountId);
        
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        PaymentResponse payment = paymentService.getPaymentById(paymentId, accountId, role);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Lấy thông tin thanh toán thành công")
                .data(payment)
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
