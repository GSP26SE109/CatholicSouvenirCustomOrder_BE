package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.InitiateStagePaymentRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.StagePaymentResponse;
import org.example.catholicsouvenircustomorder.service.CustomOrderStageService;
import org.example.catholicsouvenircustomorder.service.StagePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/stage-payments")
@RequiredArgsConstructor
public class StagePaymentController {
    
    private final StagePaymentService stagePaymentService;
    private final CustomOrderStageService stageService;
    
    /**
     * Initiate payment for a stage
     * POST /api/stage-payments/{stageId}/initiate
     * Requirements: RB-5 (Request-Based payment flow)
     */
    @PostMapping("/{stageId}/initiate")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<PaymentInitiationResponse>> initiateStagePayment(
            @PathVariable UUID stageId,
            @Valid @RequestBody InitiateStagePaymentRequest request,
            @AuthenticationPrincipal UUID customerId) {
        
        log.info("POST /api/stage-payments/{}/initiate - Customer: {}", stageId, customerId);
        
        PaymentInitiationResponse response = stageService.initiateStagePayment(stageId, request, customerId);
        
        return ResponseEntity.ok(BaseResponse.<PaymentInitiationResponse>builder()
                .code(200)
                .message("Khởi tạo thanh toán giai đoạn thành công")
                .data(response)
                .build());
    }
    
    /**
     * VNPay return endpoint for stage payments (GET method)
     * This is called by VNPay after payment, processes the payment, then redirects to frontend
     * IMPORTANT: Update DB here as backup in case IPN/callback fails or delays
     */
    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVNPayReturn(@RequestParam Map<String, String> params) {
        log.info("========================================");
        log.info("VNPay return callback for stage payment");
        log.info("Params: {}", params);
        log.info("========================================");
        
        try {
            // Extract parameters
            String referenceId = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String transactionId = params.get("vnp_TransactionNo");
            
            if (referenceId == null) {
                log.error("Missing vnp_TxnRef in return callback");
                return ResponseEntity.status(302)
                        .header("Location", "http://localhost:3000/payment/error?message=Missing_transaction_reference")
                        .build();
            }
            
            // Determine status
            String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
            
            // CRITICAL: Update DB here (backup for IPN/callback)
            // IPN may fail or delay, so we update DB immediately when user returns
            StagePaymentResponse paymentResponse = null;
            try {
                log.info("Updating stage payment status via return URL...");
                paymentResponse = stagePaymentService.handleStagePaymentCallback(referenceId, status);
                log.info("Stage payment status updated successfully via return URL");
            } catch (Exception e) {
                log.error("Error updating stage payment via return URL", e);
                // Continue to redirect even if update fails
            }
            
            // Get the saved returnUrl from payment
            String returnUrl = getReturnUrlFromPayment(referenceId);
            
            // Build redirect URL with payment result
            String redirectUrl;
            if (returnUrl != null && !returnUrl.isEmpty()) {
                redirectUrl = buildRedirectUrl(returnUrl, paymentResponse, responseCode);
            } else {
                // Default frontend URL if no returnUrl was saved
                redirectUrl = buildDefaultRedirectUrl(paymentResponse, responseCode);
            }
            
            log.info("Redirecting to: {}", redirectUrl);
            log.info("DB will also be updated by IPN/callback if configured");
            log.info("========================================");
            
            return ResponseEntity.status(302)
                    .header("Location", redirectUrl)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error handling VNPay return: ", e);
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:3000/payment/error?message=" + e.getMessage())
                    .build();
        }
    }
    
    /**
     * VNPay callback endpoint for stage payments (GET method)
     * This is for IPN (Instant Payment Notification) - optional webhook
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<BaseResponse<StagePaymentResponse>> handleVNPayCallback(
            @RequestParam Map<String, String> params) {
        
        log.info("Received VNPay callback for stage payment");
        log.info("Callback params: {}", params);
        
        // Extract parameters
        String referenceId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionId = params.get("vnp_TransactionNo");
        
        if (referenceId == null) {
            log.error("Missing vnp_TxnRef in callback");
            return ResponseEntity.badRequest().body(BaseResponse.<StagePaymentResponse>builder()
                    .code(400)
                    .message("Missing vnp_TxnRef parameter")
                    .build());
        }
        
        // Determine status
        String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
        
        try {
            StagePaymentResponse response = stagePaymentService.handleStagePaymentCallback(
                    referenceId, 
                    status
            );
            
            // Update transaction ID if available
            if (transactionId != null && response != null) {
                // Transaction ID is already set in the service
                log.info("Stage payment processed successfully. Transaction ID: {}", transactionId);
            }
            
            return ResponseEntity.ok(BaseResponse.<StagePaymentResponse>builder()
                    .code(200)
                    .message("Xử lý callback VNPay thành công")
                    .data(response)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error handling VNPay callback: ", e);
            return ResponseEntity.status(404).body(BaseResponse.<StagePaymentResponse>builder()
                    .code(404)
                    .message("Không tìm thấy payment: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * ZaloPay callback endpoint for stage payments (POST method)
     */
    @PostMapping("/zalopay/callback")
    public ResponseEntity<BaseResponse<StagePaymentResponse>> handleZaloPayCallback(
            @RequestBody Map<String, String> params) {
        
        log.info("Received ZaloPay callback for stage payment");
        log.info("Callback params: {}", params);
        
        // Extract parameters
        String referenceId = params.get("apptransid");
        String statusStr = params.get("status");
        String transactionId = params.get("zptransid");
        
        if (referenceId == null) {
            log.error("Missing apptransid in callback");
            return ResponseEntity.badRequest().body(BaseResponse.<StagePaymentResponse>builder()
                    .code(400)
                    .message("Missing apptransid parameter")
                    .build());
        }
        
        // Determine status
        String status = "1".equals(statusStr) ? "SUCCESS" : "FAILED";
        
        try {
            StagePaymentResponse response = stagePaymentService.handleStagePaymentCallback(
                    referenceId, 
                    status
            );
            
            if (transactionId != null && response != null) {
                log.info("Stage payment processed successfully. Transaction ID: {}", transactionId);
            }
            
            return ResponseEntity.ok(BaseResponse.<StagePaymentResponse>builder()
                    .code(200)
                    .message("Xử lý callback ZaloPay thành công")
                    .data(response)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error handling ZaloPay callback: ", e);
            return ResponseEntity.status(404).body(BaseResponse.<StagePaymentResponse>builder()
                    .code(404)
                    .message("Không tìm thấy payment: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Helper method to get returnUrl from payment
     */
    private String getReturnUrlFromPayment(String referenceId) {
        try {
            return stagePaymentService.getReturnUrlByReferenceId(referenceId);
        } catch (Exception e) {
            log.warn("Could not get returnUrl for payment: {}", referenceId);
            return null;
        }
    }
    
    /**
     * Build redirect URL with payment result parameters
     */
    private String buildRedirectUrl(String baseUrl, StagePaymentResponse payment, String responseCode) {
        StringBuilder url = new StringBuilder(baseUrl);
        
        // Add query separator
        url.append(baseUrl.contains("?") ? "&" : "?");
        
        // Add payment result parameters
        url.append("paymentId=").append(payment.getPaymentId());
        url.append("&status=").append(payment.getPaymentStatus());
        url.append("&responseCode=").append(responseCode);
        
        if (payment.getTransactionId() != null) {
            url.append("&transactionId=").append(payment.getTransactionId());
        }
        
        if (payment.getStageId() != null) {
            url.append("&stageId=").append(payment.getStageId());
        }
        
        return url.toString();
    }
    
    /**
     * Build default redirect URL when no returnUrl is provided
     */
    private String buildDefaultRedirectUrl(StagePaymentResponse payment, String responseCode) {
        String baseUrl = "http://localhost:3000/stage-payment/result";
        return buildRedirectUrl(baseUrl, payment, responseCode);
    }
}
