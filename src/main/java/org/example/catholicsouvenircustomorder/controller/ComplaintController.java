package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.CreateComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateShipmentRequest;
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
 * Controller for Customer complaint operations
 * Handles complaint creation, viewing, and return shipment creation
 * Requirements: 1.1-1.7, 7.1-7.5
 */
@Slf4j
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {
    
    private final ComplaintService complaintService;
    private final ShippingService shippingService;
    
    /**
     * Create a new complaint
     * POST /api/complaints
     * Requirements: 1.1-1.7
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<ComplaintResponse>> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} creating complaint for order: {}, customOrder: {}", 
                customerId, request.getOrderId(), request.getCustomOrderId());
        
        ComplaintResponse response = complaintService.createComplaint(request, customerId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Tạo đơn khiếu nại thành công. Artisan sẽ được thông báo để xem xét.",
                response
        ));
    }
    
    /**
     * Get my complaints with pagination
     * GET /api/complaints?page={page}&size={size}
     * Requirements: 7.1-7.5
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<Page<ComplaintResponse>>> getMyComplaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} fetching complaints, page: {}, size: {}", customerId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ComplaintResponse> response = complaintService.getCustomerComplaints(customerId, pageable);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy danh sách đơn khiếu nại thành công",
                response
        ));
    }
    
    /**
     * Get complaint details
     * GET /api/complaints/{id}
     * Requirements: 7.1-7.5
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<ComplaintDetailResponse>> getComplaintDetails(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} fetching complaint details: {}", customerId, id);
        
        ComplaintDetailResponse response = complaintService.getComplaintDetails(id, customerId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy chi tiết đơn khiếu nại thành công",
                response
        ));
    }
    
    /**
     * Create return shipment for complaint (optional)
     * POST /api/complaints/{id}/return
     * Requirements: 5.1, 5.2, 5.3
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<ShipmentResponse>> createReturnShipment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateShipmentRequest request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} creating return shipment for complaint: {}", customerId, id);
        
        ShipmentResponse response = shippingService.createReturnShipment(id, request, customerId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Tạo đơn trả hàng thành công. Vui lòng gửi hàng về địa chỉ của Artisan.",
                response
        ));
    }
}
