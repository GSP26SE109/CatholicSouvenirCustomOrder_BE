package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.ApproveWithdrawalRequest;
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
@RequestMapping("/api/admin/withdrawals")
@RequiredArgsConstructor
public class AdminWithdrawalController {
    
    private final WithdrawalService withdrawalService;
    
    // ==================== ADMIN ENDPOINTS ====================
    
    /**
     * Get all withdrawal requests with filtering and pagination
     * GET /api/admin/withdrawals
     * Requirements: 4.1, 5.1, 6.1, 7.1
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get all withdrawals", description = "Admin gets all withdrawal requests with filtering")
    public ResponseEntity<BaseResponse<Page<WithdrawalResponse>>> getAllWithdrawals(
            @ModelAttribute WithdrawalFilterRequest filter,
            Authentication authentication) {
        
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("GET /api/admin/withdrawals - Admin: {}, Filter: {}", adminId, filter);
        
        Page<WithdrawalResponse> response = withdrawalService.getAllWithdrawals(filter);
        
        return ResponseEntity.ok(BaseResponse.<Page<WithdrawalResponse>>builder()
                .code(200)
                .message("Lấy danh sách yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }
    
    /**
     * Get withdrawal detail
     * GET /api/admin/withdrawals/{id}
     * Requirements: 7.1
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get withdrawal detail", description = "Admin gets withdrawal request detail")
    public ResponseEntity<BaseResponse<WithdrawalDetailResponse>> getWithdrawalDetail(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("GET /api/admin/withdrawals/{} - Admin: {}", id, adminId);
        
        WithdrawalDetailResponse response = withdrawalService.getWithdrawalDetail(adminId, id, true);
        
        return ResponseEntity.ok(BaseResponse.<WithdrawalDetailResponse>builder()
                .code(200)
                .message("Lấy chi tiết yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }
    
    /**
     * Approve a withdrawal request
     * POST /api/admin/withdrawals/{id}/approve
     * Requirements: 5.1
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Approve withdrawal", description = "Admin approves a withdrawal request")
    public ResponseEntity<BaseResponse<WithdrawalResponse>> approveWithdrawal(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveWithdrawalRequest request,
            Authentication authentication) {
        
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("POST /api/admin/withdrawals/{}/approve - Admin: {}", id, adminId);
        
        WithdrawalResponse response = withdrawalService.approveWithdrawal(adminId, id, request);
        
        return ResponseEntity.ok(BaseResponse.<WithdrawalResponse>builder()
                .code(200)
                .message("Phê duyệt yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }
    
    /**
     * Reject a withdrawal request
     * POST /api/admin/withdrawals/{id}/reject
     * Requirements: 6.1
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Reject withdrawal", description = "Admin rejects a withdrawal request")
    public ResponseEntity<BaseResponse<WithdrawalResponse>> rejectWithdrawal(
            @PathVariable UUID id,
            @Valid @RequestBody RejectWithdrawalRequest request,
            Authentication authentication) {
        
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("POST /api/admin/withdrawals/{}/reject - Admin: {}", id, adminId);
        
        WithdrawalResponse response = withdrawalService.rejectWithdrawal(adminId, id, request);
        
        return ResponseEntity.ok(BaseResponse.<WithdrawalResponse>builder()
                .code(200)
                .message("Từ chối yêu cầu rút tiền thành công")
                .data(response)
                .build());
    }
}
