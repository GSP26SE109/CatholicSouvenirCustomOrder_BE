package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.dto.response.VNPayCallbackResponse;
import org.example.catholicsouvenircustomorder.dto.response.ZaloPayCallbackResponse;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/stages/{stageId}/create-payment")
    public ResponseEntity<BaseResponse<PaymentResponse>> createPayment(
            @PathVariable UUID stageId,
            @RequestParam PaymentMethod method,
            Authentication authentication) {
        
        // Get customerId from authentication if available, otherwise use provided customerId
        UUID actualCustomerId = UUID.fromString(authentication.getName());
            
        if (actualCustomerId == null) {
            return ResponseEntity.badRequest().body(BaseResponse.<PaymentResponse>builder()
                    .code(400)
                    .message("Yêu cầu ID khách hàng")
                    .build());
        }
        
        PaymentResponse response = paymentService.createPayment(stageId, method, actualCustomerId);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Tạo thanh toán thành công")
                .data(response)
                .build());
    }
    
    @PostMapping("/zalopay/callback")
    public ResponseEntity<ZaloPayCallbackResponse> zaloPayCallback(@RequestBody Map<String, String> callbackData) {
        try {
            log.info("Received ZaloPay callback: {}", callbackData);
            paymentService.handleZaloPayCallback(callbackData);
            
            return ResponseEntity.ok(ZaloPayCallbackResponse.builder()
                    .returnCode(1)
                    .returnMessage("success")
                    .build());
        } catch (Exception e) {
            log.error("ZaloPay callback error", e);
            return ResponseEntity.ok(ZaloPayCallbackResponse.builder()
                    .returnCode(-1)
                    .returnMessage("failed")
                    .build());
        }
    }
    
    @GetMapping("/vnpay/callback")
    public ResponseEntity<VNPayCallbackResponse> vnPayCallback(@RequestParam Map<String, String> callbackData) {
        try {
            log.info("Received VNPay callback: {}", callbackData);
            paymentService.handleVNPayCallback(callbackData);
            
            return ResponseEntity.ok(VNPayCallbackResponse.builder()
                    .RspCode("00")
                    .Message("success")
                    .build());
        } catch (Exception e) {
            log.error("VNPay callback error", e);
            return ResponseEntity.ok(VNPayCallbackResponse.builder()
                    .RspCode("99")
                    .Message("failed")
                    .build());
        }
    }
    
    @GetMapping("/stages/{stageId}")
    public ResponseEntity<BaseResponse<PaymentResponse>> getPaymentByStage(@PathVariable UUID stageId) {
        PaymentResponse response = paymentService.getPaymentByStageId(stageId);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Lấy thông tin thanh toán thành công")
                .data(response)
                .build());
    }
}
