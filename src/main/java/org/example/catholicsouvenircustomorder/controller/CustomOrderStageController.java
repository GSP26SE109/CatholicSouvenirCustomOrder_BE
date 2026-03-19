package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.service.CustomOrderStageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class CustomOrderStageController {
    
    private final CustomOrderStageService stageService;
    
    @GetMapping("/{stageId}")
    public ResponseEntity<BaseResponse<CustomOrderStageResponse>> getStageById(@PathVariable UUID stageId) {
        CustomOrderStageResponse response = stageService.getStageById(stageId);
        
        return ResponseEntity.ok(BaseResponse.<CustomOrderStageResponse>builder()
                .code(200)
                .message("Stage retrieved successfully")
                .data(response)
                .build());
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<BaseResponse<List<CustomOrderStageResponse>>> getStagesByOrderId(@PathVariable UUID orderId) {
        List<CustomOrderStageResponse> response = stageService.getStagesByOrderId(orderId);
        
        return ResponseEntity.ok(BaseResponse.<List<CustomOrderStageResponse>>builder()
                .code(200)
                .message("Stages retrieved successfully")
                .data(response)
                .build());
    }
    
    @PostMapping("/{stageId}/complete")
    public ResponseEntity<BaseResponse<CustomOrderStageResponse>> completeStage(
            @PathVariable UUID stageId,
            @RequestBody CompleteStageRequest request,
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        CustomOrderStageResponse response = stageService.completeStage(stageId, request, artisanId);
        
        return ResponseEntity.ok(BaseResponse.<CustomOrderStageResponse>builder()
                .code(200)
                .message("Stage completed successfully")
                .data(response)
                .build());
    }
    
    @GetMapping("/{stageId}/can-pay")
    public ResponseEntity<BaseResponse<Boolean>> canPayStage(@PathVariable UUID stageId) {
        boolean canPay = stageService.canPayStage(stageId);
        
        return ResponseEntity.ok(BaseResponse.<Boolean>builder()
                .code(200)
                .message(canPay ? "Stage can be paid" : "Stage cannot be paid yet")
                .data(canPay)
                .build());
    }
}
