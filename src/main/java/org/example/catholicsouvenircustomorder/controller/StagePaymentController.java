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
     * VNPay callback endpoint for stage payments (GET method)
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
}
