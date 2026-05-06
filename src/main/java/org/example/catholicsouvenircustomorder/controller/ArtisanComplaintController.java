package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.ArtisanResponseRequest;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintResponse;
import org.example.catholicsouvenircustomorder.dto.response.ShipmentResponse;
import org.example.catholicsouvenircustomorder.service.ComplaintService;
import org.example.catholicsouvenircustomorder.service.ShippingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for Artisan complaint operations
 * Handles viewing complaints, responding, and confirming return receipt
 * Requirements: 2.1-2.8, 8.1-8.3
 */
@Slf4j
@RestController
@RequestMapping("/api/artisan/complaints")
@RequiredArgsConstructor
public class ArtisanComplaintController {
    
    private final ComplaintService complaintService;
    private final ShippingService shippingService;
    
    /**
     * Get my complaints with pagination
     * GET /api/artisan/complaints?page={page}&size={size}
     * Requirements: 8.1-8.3
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<Page<ComplaintResponse>>> getMyComplaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("Artisan {} fetching complaints, page: {}, size: {}", artisanId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ComplaintResponse> response = complaintService.getArtisanComplaints(artisanId, pageable);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy danh sách đơn khiếu nại thành công",
                response
        ));
    }
    
    /**
     * Get complaint details
     * GET /api/artisan/complaints/{id}
     * Requirements: 8.1-8.3
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<ComplaintDetailResponse>> getComplaintDetails(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("Artisan {} fetching complaint details: {}", artisanId, id);
        
        ComplaintDetailResponse response = complaintService.getComplaintDetails(id, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy chi tiết đơn khiếu nại thành công",
                response
        ));
    }
    
    /**
     * Respond to complaint
     * POST /api/artisan/complaints/{id}/respond
     * Requirements: 2.1-2.8
     */
    @PostMapping("/{id}/respond")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<ComplaintResponse>> respondToComplaint(
            @PathVariable UUID id,
            @Valid @RequestBody ArtisanResponseRequest request,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("Artisan {} responding to complaint: {}", artisanId, id);
        
        ComplaintResponse response = complaintService.respondToComplaint(id, request, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Phản hồi đơn khiếu nại thành công. Admin sẽ xem xét và đưa ra quyết định.",
                response
        ));
    }
    
    /**
     * Confirm return receipt (optional)
     * POST /api/artisan/return-shipments/{id}/confirm
     * Requirements: 5.4, 5.5, 5.6
     */
    @PostMapping("/return-shipments/{id}/confirm")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<ShipmentResponse>> confirmReturnReceipt(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("Artisan {} confirming return receipt for shipment: {}", artisanId, id);
        
        ShipmentResponse response = shippingService.confirmReturnReceipt(id, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Xác nhận nhận hàng trả về thành công. Hệ thống sẽ xử lý hoàn tiền.",
                response
        ));
    }
}
