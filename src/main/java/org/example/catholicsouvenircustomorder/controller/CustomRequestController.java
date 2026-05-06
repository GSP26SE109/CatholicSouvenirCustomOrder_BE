package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateFreeFormRequestDTO;
import org.example.catholicsouvenircustomorder.dto.request.SelectArtisanRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateDraftRequestDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;
import org.example.catholicsouvenircustomorder.service.CustomRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for Request-Based Custom Orders.
 * CustomRequest is only used for request-based flow (not template-based).
 */
@Slf4j
@RestController
@RequestMapping("/api/custom-requests")
@RequiredArgsConstructor
public class CustomRequestController {
    
    private final CustomRequestService customRequestService;
    
    /**
     * Create a new custom request (draft status)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> createCustomRequest(
            @Valid @RequestBody CreateFreeFormRequestDTO request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.createFreeFormRequest(request, customerId);
        return ResponseEntity.ok(BaseResponse.success("Tạo yêu cầu thành công", response));
    }
    
    /**
     * Update a draft custom request (title and description only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> updateDraftRequest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDraftRequestDTO request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.updateDraftRequest(id, request, customerId);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật yêu cầu thành công", response));
    }
    
    /**
     * Publish a draft request to make it visible to artisans
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> publishRequest(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.publishRequest(id, customerId);
        return ResponseEntity.ok(BaseResponse.success("Đăng yêu cầu thành công", response));
    }
    
    /**
     * Get customer's own requests
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> getCustomerRequests(
            @RequestParam(required = false) CustomRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomRequestResponse> response = customRequestService.getCustomerRequests(customerId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách yêu cầu thành công", response));
    }
    
    /**
     * Get open requests (for artisans to browse)
     * Returns both OPEN (available) and ARTISAN_SELECTED (already taken) requests
     * This provides transparency so artisans can see which requests are still available
     */
    @GetMapping("/open")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> getOpenRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomRequestResponse> response = customRequestService.getOpenRequests(pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách yêu cầu mở thành công", response));
    }
    
    /**
     * Customer selects an artisan for their request
     */
    @PostMapping("/{id}/select-artisan")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> selectArtisan(
            @PathVariable UUID id,
            @Valid @RequestBody SelectArtisanRequest request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.selectArtisan(id, request.getArtisanId(), customerId);
        return ResponseEntity.ok(BaseResponse.success("Chọn nghệ nhân thành công", response));
    }
    
    /**
     * Get request detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getCustomRequestDetail(@PathVariable UUID id) {
        CustomRequestResponse response = customRequestService.getRequestDetail(id);
        return ResponseEntity.ok(BaseResponse.success("Lấy chi tiết yêu cầu thành công", response));
    }
    
    /**
     * Regenerate AI concept image
     */
    @PostMapping("/{id}/regenerate-image")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> regenerateAIImage(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.regenerateAIImage(id, customerId);
        return ResponseEntity.ok(BaseResponse.success("Tạo lại ảnh AI thành công", response));
    }
    
    /**
     * Get artisan's requests (where artisan is selected)
     */
    @GetMapping("/artisan")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> getArtisanCustomRequests(
            @RequestParam(required = false) CustomRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomRequestResponse> response = customRequestService.getArtisanRequests(artisanId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách yêu cầu thành công", response));
    }
    
    /**
     * Delete a draft custom request
     * Only DRAFT status requests can be deleted
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> deleteCustomRequest(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        customRequestService.deleteDraftRequest(id, customerId);
        return ResponseEntity.ok(BaseResponse.success("Xóa yêu cầu thành công", null));
    }
}
