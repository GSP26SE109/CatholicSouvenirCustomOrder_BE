package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.config.VNPayConfig;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.InitiateStagePaymentRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.StagePaymentResponse;
import org.example.catholicsouvenircustomorder.service.CustomOrderStageService;
import org.example.catholicsouvenircustomorder.service.StagePaymentService;
import org.example.catholicsouvenircustomorder.util.VNPayUtil;
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
    private final VNPayUtil vnPayUtil;
    private final VNPayConfig vnPayConfig;
    
    @org.springframework.beans.factory.annotation.Value("${app.frontend-url}")
    private String frontendUrl;
    
    /**
     * Initiate payment for a stage
     * POST /api/stage-payments/{stageId}/initiate
     * Requirements: RB-5 (Request-Based payment flow)
     * 
     * returnUrl supports:
     * - Web: https://your-domain.com/payment/result
     * - Mobile deep link: myapp://payment/result
     * - Custom scheme: yourapp://callback
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
        try {
            log.info("Received VNPay return callback");
            
            String referenceId = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String transactionId = params.get("vnp_TransactionNo");
            String vnpAmount = params.get("vnp_Amount");
            
            // VNPay returns amount in smallest unit (multiplied by 100), convert back to actual amount
            String actualAmount = vnpAmount;
            try {
                long amountInSmallestUnit = Long.parseLong(vnpAmount);
                actualAmount = String.valueOf(amountInSmallestUnit / 100);
            } catch (NumberFormatException e) {
                log.warn("Could not parse vnp_Amount: {}", vnpAmount);
            }
            
            if (referenceId == null) {
                log.error("Missing vnp_TxnRef in return callback");
                String errorUrl = frontendUrl + "/payment/error?message=Missing_transaction_reference";
                return ResponseEntity.status(302)
                        .header("Location", errorUrl)
                        .build();
            }
            
            String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
            
            StagePaymentResponse paymentResponse = null;
            try {
                paymentResponse = stagePaymentService.handleStagePaymentCallback(referenceId, status, transactionId);
            } catch (Exception e) {
                log.error("Error updating stage payment for ref: {}", referenceId, e);
            }
            
            String returnUrl = getReturnUrlFromPayment(referenceId);
            
            String redirectUrl;
            if (paymentResponse != null && returnUrl != null && !returnUrl.isEmpty()) {
                redirectUrl = buildRedirectUrl(returnUrl, paymentResponse, responseCode, actualAmount);
            } else if (paymentResponse != null) {
                redirectUrl = buildDefaultRedirectUrl(paymentResponse, responseCode, actualAmount);
            } else {
                log.error("Payment update failed for ref: {}", referenceId);
                redirectUrl = frontendUrl + "/payment/error?message=Payment_update_failed&ref=" + referenceId;
            }
            
            return ResponseEntity.status(302)
                    .header("Location", redirectUrl)
                    .build();
                    
        } catch (Exception e) {
            log.error("Unexpected error handling VNPay return", e);
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
        
        log.info("Received VNPay IPN callback for stage payment");
        
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
                return ResponseEntity.badRequest().body(BaseResponse.<StagePaymentResponse>builder()
                        .code(400)
                        .message("Chữ ký không hợp lệ")
                        .build());
            }
            
            String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
            
            StagePaymentResponse response = stagePaymentService.handleStagePaymentCallback(
                    referenceId, 
                    status,
                    transactionId
            );
            
            if (transactionId != null && response != null) {
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
     * Supports both web URLs and mobile deep links
     * 
     * Examples:
     * - Web: https://domain.com/result?paymentId=xxx&status=SUCCESS&amount=100000
     * - Mobile: myapp://payment/result?paymentId=xxx&status=SUCCESS&amount=100000
     */
    private String buildRedirectUrl(String baseUrl, StagePaymentResponse payment, String responseCode, String amount) {
        StringBuilder url = new StringBuilder(baseUrl);
        
        // Add query separator
        url.append(baseUrl.contains("?") ? "&" : "?");
        
        // Add payment result parameters
        url.append("paymentId=").append(payment.getPaymentId());
        url.append("&status=").append(payment.getPaymentStatus());
        url.append("&responseCode=").append(responseCode);
        url.append("&amount=").append(amount);
        
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
    private String buildDefaultRedirectUrl(StagePaymentResponse payment, String responseCode, String amount) {
        String baseUrl = frontendUrl + "/stage-payment/result";
        return buildRedirectUrl(baseUrl, payment, responseCode, amount);
    }
}
