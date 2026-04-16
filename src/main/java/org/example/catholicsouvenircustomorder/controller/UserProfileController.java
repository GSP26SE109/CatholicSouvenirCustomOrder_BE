package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.UpdateUserProfileRequest;
import org.example.catholicsouvenircustomorder.dto.response.UserProfileResponse;
import org.example.catholicsouvenircustomorder.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {
    
    private final UserProfileService userProfileService;
    
    /**
     * Get current user's profile
     * GET /api/profile
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile", description = "Get current user's profile information")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
        UUID accountId = (UUID) authentication.getPrincipal();
        log.info("GET /api/profile - User: {}", accountId);
        
        UserProfileResponse response = userProfileService.getUserProfile(accountId);
        
        return ResponseEntity.ok(BaseResponse.<UserProfileResponse>builder()
                .code(200)
                .message("Lấy thông tin profile thành công")
                .data(response)
                .build());
    }
    
    /**
     * Get user profile by account ID (Admin only)
     * GET /api/profile/{accountId}
     */
    @GetMapping("/{accountId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get user profile by ID", description = "Admin gets any user's profile by account ID")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getUserProfileById(
            @PathVariable UUID accountId) {
        log.info("GET /api/profile/{} - Admin request", accountId);
        
        UserProfileResponse response = userProfileService.getUserProfile(accountId);
        
        return ResponseEntity.ok(BaseResponse.<UserProfileResponse>builder()
                .code(200)
                .message("Lấy thông tin profile thành công")
                .data(response)
                .build());
    }
    
    /**
     * Update current user's profile
     * PUT /api/profile
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my profile", description = "Update current user's profile information")
    public ResponseEntity<BaseResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request,
            Authentication authentication) {
        UUID accountId = (UUID) authentication.getPrincipal();
        log.info("PUT /api/profile - User: {}", accountId);
        
        UserProfileResponse response = userProfileService.updateUserProfile(accountId, request);
        
        return ResponseEntity.ok(BaseResponse.<UserProfileResponse>builder()
                .code(200)
                .message("Cập nhật profile thành công")
                .data(response)
                .build());
    }
    
    /**
     * Partially update current user's profile
     * PATCH /api/profile
     */
    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Partially update my profile", description = "Partially update current user's profile (only provided fields)")
    public ResponseEntity<BaseResponse<UserProfileResponse>> patchMyProfile(
            @RequestBody UpdateUserProfileRequest request,
            Authentication authentication) {
        UUID accountId = (UUID) authentication.getPrincipal();
        log.info("PATCH /api/profile - User: {}", accountId);
        
        UserProfileResponse response = userProfileService.updateUserProfile(accountId, request);
        
        return ResponseEntity.ok(BaseResponse.<UserProfileResponse>builder()
                .code(200)
                .message("Cập nhật profile thành công")
                .data(response)
                .build());
    }
    
    /**
     * Delete current user's profile (Admin only)
     * DELETE /api/profile/{accountId}
     */
    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete user profile", description = "Admin deletes a user's profile")
    public ResponseEntity<BaseResponse<Void>> deleteUserProfile(@PathVariable UUID accountId) {
        log.info("DELETE /api/profile/{} - Admin request", accountId);
        
        userProfileService.deleteUserProfile(accountId);
        
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .code(200)
                .message("Xóa profile thành công")
                .build());
    }
}
