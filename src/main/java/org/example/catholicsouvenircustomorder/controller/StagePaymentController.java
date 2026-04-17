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
    private final org.example.catholicsouvenircustomorder.util.VNPayUtil vnPayUtil;
    private final org.example.catholicsouvenircustomorder.config.VNPayConfig vnPayConfig;
    
    @org.springframework.beans.factory.annotation.Value("${app.frontend-url}")
    private String frontendUrl;
    
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
                // Use configured frontend URL for error redirect
                String errorUrl = frontendUrl + "/payment/error?message=Missing_transaction_reference";
                return ResponseEntity.status(302)
                        .header("Location", errorUrl)
                        .build();
            }
            
            // Determine status
            String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
            
            // CRITICAL: Update DB here (backup for IPN/callback)
            // IPN may fail or delay, so we update DB immediately when user returns
            StagePaymentResponse paymentResponse = null;
            try {
                log.info("Updating stage payment status via return URL...");
                log.info("Reference ID: {}", referenceId);
                log.info("Response Code: {}", responseCode);
                log.info("Transaction ID: {}", transactionId);
                log.info("Status: {}", status);
                
                paymentResponse = stagePaymentService.handleStagePaymentCallback(referenceId, status);
                log.info("Stage payment status updated successfully via return URL");
            } catch (Exception e) {
                log.error("Error updating stage payment via return URL", e);
                log.error("Exception type: {}", e.getClass().getName());
                log.error("Exception message: {}", e.getMessage());
                // Continue to redirect even if update fails
            }
            
            // Get the saved returnUrl from payment
            String returnUrl = getReturnUrlFromPayment(referenceId);
            
            // Build redirect URL with payment result
            String redirectUrl;
            if (paymentResponse != null && returnUrl != null && !returnUrl.isEmpty()) {
                redirectUrl = buildRedirectUrl(returnUrl, paymentResponse, responseCode);
            } else if (paymentResponse != null) {
                // Default frontend URL if no returnUrl was saved
                redirectUrl = buildDefaultRedirectUrl(paymentResponse, responseCode);
            } else {
                // Payment update failed, redirect to error page
                log.error("Payment response is null, redirecting to error page");
                redirectUrl = frontendUrl + "/payment/error?message=Payment_update_failed&ref=" + referenceId;
            }
            
            log.info("Redirecting to: {}", redirectUrl);
            log.info("DB will also be updated by IPN/callback if configured");
            log.info("========================================");
            
            return ResponseEntity.status(302)
                    .header("Location", redirectUrl)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error handling VNPay return: ", e);
            // Use configured frontend URL for error redirect
            String errorUrl = frontendUrl + "/payment/error?message=" + e.getMessage();
            return ResponseEntity.status(302)
                    .header("Location", errorUrl)
                    .build();
        }
    }
    
    /**
     * VNPay callback endpoint for stage payments (GET method)
     * This is for IPN (Instant Payment Notification) - server-to-server callback
     * IMPORTANT: This is the authoritative source - VNPay guarantees delivery
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<BaseResponse<StagePaymentResponse>> handleVNPayCallback(
            @RequestParam Map<String, String> params) {
        
        log.info("========================================");
        log.info("Received VNPay IPN callback for stage payment");
        log.info("All callback params: {}", params);
        log.info("========================================");
        
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
        
        try {
            // CRITICAL: Verify VNPay signature first
            log.info("Verifying VNPay signature...");
            
            // Filter only VNPay params (vnp_*)
            Map<String, String> vnpParams = new java.util.HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("vnp_")) {
                    vnpParams.put(entry.getKey(), entry.getValue());
                }
            }
            
            boolean isValidSignature = vnPayUtil.verifySecureHash(
                vnpParams, 
                vnPayConfig.getHashSecret()
            );
            
            if (!isValidSignature) {
                log.error("VNPay signature verification FAILED!");
                log.error("Received hash: {}", params.get("vnp_SecureHash"));
                return ResponseEntity.badRequest().body(BaseResponse.<StagePaymentResponse>builder()
                        .code(400)
                        .message("Chữ ký không hợp lệ")
                        .build());
            }
            
            log.info("VNPay signature verified successfully");
            
            // Determine status
            String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
            
            StagePaymentResponse response = stagePaymentService.handleStagePaymentCallback(
                    referenceId, 
                    status
            );
            
            // Update transaction ID if available
            if (transactionId != null && response != null) {
                log.info("Stage payment processed successfully. Transaction ID: {}", transactionId);
            }
            
            log.info("IPN callback processing completed successfully");
            log.info("========================================");
            
            return ResponseEntity.ok(BaseResponse.<StagePaymentResponse>builder()
                    .code(200)
                    .message("Xử lý callback VNPay thành công")
                    .data(response)
                    .build());
                    
        } catch (Exception e) {
            log.error("========================================");
            log.error("Error handling VNPay callback: ", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("========================================");
            
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
     * Uses configured frontend URL from application.yml
     */
    private String buildDefaultRedirectUrl(StagePaymentResponse payment, String responseCode) {
        // Use configured frontend URL instead of hardcoded localhost
        String baseUrl = frontendUrl + "/stage-payment/result";
        log.warn("No returnUrl found in payment, using default: {}", baseUrl);
        return buildRedirectUrl(baseUrl, payment, responseCode);
    }
}
