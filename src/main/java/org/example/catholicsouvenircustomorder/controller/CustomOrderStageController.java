package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.service.CustomOrderStageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomOrderStageController {
    
    private final CustomOrderStageService stageService;
    
    @GetMapping("/stages/{stageId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<CustomOrderStageResponse>> getStageById(
            @PathVariable UUID stageId,
            @AuthenticationPrincipal UUID userId) {
        
        log.info("GET /api/stages/{} - User: {}", stageId, userId);
        
        CustomOrderStageResponse response = stageService.getStageById(stageId, userId);
        
        return ResponseEntity.ok(BaseResponse.<CustomOrderStageResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Stage retrieved successfully")
                .data(response)
                .build());
    }
    
    @GetMapping("/custom-orders/{orderId}/stages")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<List<CustomOrderStageResponse>>> getStagesByOrderId(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID userId) {
        
        log.info("GET /api/custom-orders/{}/stages - User: {}", orderId, userId);
        
        List<CustomOrderStageResponse> response = stageService.getStagesByOrderId(orderId, userId);
        
        return ResponseEntity.ok(BaseResponse.<List<CustomOrderStageResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Stages retrieved successfully")
                .data(response)
                .build());
    }
    
    @PostMapping("/stages/{stageId}/complete")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<CustomOrderStageResponse>> completeStage(
            @PathVariable UUID stageId,
            @Valid @RequestBody CompleteStageRequest request,
            @AuthenticationPrincipal UUID artisanId) {
        
        log.info("POST /api/stages/{}/complete - Artisan: {}", stageId, artisanId);
        
        CustomOrderStageResponse response = stageService.completeStage(stageId, request, artisanId);
        
        return ResponseEntity.ok(BaseResponse.<CustomOrderStageResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Stage completed successfully")
                .data(response)
                .build());
    }
    
    @PostMapping("/stages/{stageId}/upload-proof")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<CustomOrderStageResponse>> uploadProofImage(
            @PathVariable UUID stageId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UUID artisanId) {
        
        log.info("POST /api/stages/{}/upload-proof - Artisan: {}", stageId, artisanId);
        
        String imageUrl = request.get("imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) {
            return ResponseEntity.badRequest().body(BaseResponse.<CustomOrderStageResponse>builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("Image URL is required")
                    .build());
        }
        
        CustomOrderStageResponse response = stageService.uploadProofImage(stageId, imageUrl, artisanId);
        
        return ResponseEntity.ok(BaseResponse.<CustomOrderStageResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Proof image uploaded successfully")
                .data(response)
                .build());
    }
    
    @PostMapping("/stages/{stageId}/payment/initiate")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<PaymentInitiationResponse>> initiateStagePayment(
            @PathVariable UUID stageId,
            @Valid @RequestBody InitiatePaymentDTO request,
            @AuthenticationPrincipal UUID customerId) {
        
        log.info("POST /api/stages/{}/payment/initiate - Customer: {}", stageId, customerId);
        
        PaymentInitiationResponse response = stageService.initiateStagePayment(stageId, request, customerId);
        
        return ResponseEntity.ok(BaseResponse.<PaymentInitiationResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Payment initiated successfully")
                .data(response)
                .build());
    }
    
    @GetMapping("/stages/{stageId}/can-pay")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<Boolean>> canPayStage(@PathVariable UUID stageId) {
        
        log.info("GET /api/stages/{}/can-pay", stageId);
        
        boolean canPay = stageService.canPayStage(stageId);
        
        return ResponseEntity.ok(BaseResponse.<Boolean>builder()
                .code(HttpStatus.OK.value())
                .message(canPay ? "Stage can be paid" : "Stage cannot be paid yet")
                .data(canPay)
                .build());
    }
}
