package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.ApproveWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.RejectWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.WithdrawalFilterRequest;
import org.example.catholicsouvenircustomorder.dto.response.WithdrawalDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.WithdrawalResponse;
import org.example.catholicsouvenircustomorder.service.WithdrawalService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {
    
    private final WithdrawalService withdrawalService;
    
    // ==================== ARTISAN ENDPOINTS ====================
    
    /**
     * Create a new withdrawal request
     * POST /api/withdrawals
     * Requirements: 1.1, 2.1, 3.1, 7.1
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ARTISAN')")
    @Operation(summary = "Create withdrawal request", description = "Artisan creates a new withdrawal request")
    public ResponseEntity<BaseResponse<WithdrawalResponse>> createWithdrawalRequest(
            @Valid @RequestBody CreateWithdrawalRequest request,
            Authentication authentication) {
        
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("POST /api/withdrawals - Artisan: {}, Amount: {}", artisanId, request.getAmount());
        
        WithdrawalResponse response = withdrawalService.createWithdrawalRequest(artisanId, request);
        
        return ResponseEntity.ok(BaseResponse.<WithdrawalResponse>builder()
                .code(200)
                .message("Tạo yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }
    
    /**
     * Get my withdrawal requests with filtering and pagination
     * GET /api/withdrawals/my
     * Requirements: 2.1
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ARTISAN')")
    @Operation(summary = "Get my withdrawals", description = "Artisan gets their withdrawal requests")
    public ResponseEntity<BaseResponse<Page<WithdrawalResponse>>> getMyWithdrawals(
            @ModelAttribute WithdrawalFilterRequest filter,
            Authentication authentication) {
        
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("GET /api/withdrawals/my - Artisan: {}, Filter: {}", artisanId, filter);
        
        Page<WithdrawalResponse> response = withdrawalService.getWithdrawalsByArtisan(artisanId, filter);
        
        return ResponseEntity.ok(BaseResponse.<Page<WithdrawalResponse>>builder()
                .code(200)
                .message("Lấy danh sách yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }
    
    /**
     * Get withdrawal detail
     * GET /api/withdrawals/{id}
     * Requirements: 7.1
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ARTISAN')")
    @Operation(summary = "Get withdrawal detail", description = "Artisan gets withdrawal request detail")
    public ResponseEntity<BaseResponse<WithdrawalDetailResponse>> getWithdrawalDetail(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("GET /api/withdrawals/{} - Artisan: {}", id, artisanId);
        
        WithdrawalDetailResponse response = withdrawalService.getWithdrawalDetail(artisanId, id, false);
        
        return ResponseEntity.ok(BaseResponse.<WithdrawalDetailResponse>builder()
                .code(200)
                .message("Lấy chi tiết yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }
    
    /**
     * Cancel a pending withdrawal request
     * DELETE /api/withdrawals/{id}/cancel
     * Requirements: 3.1
     */
    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ARTISAN')")
    @Operation(summary = "Cancel withdrawal", description = "Artisan cancels a pending withdrawal request")
    public ResponseEntity<BaseResponse<Void>> cancelWithdrawal(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("DELETE /api/withdrawals/{}/cancel - Artisan: {}", id, artisanId);
        
        withdrawalService.cancelWithdrawal(artisanId, id);
        
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .code(200)
                .message("Hủy yêu cầu rút tiền thành công")
                .build());
    }
}
