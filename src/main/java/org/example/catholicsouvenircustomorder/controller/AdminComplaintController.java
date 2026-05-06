package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.ApproveComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.RejectComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintResponse;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.RefundTransactionResponse;
import org.example.catholicsouvenircustomorder.model.ComplaintStatus;
import org.example.catholicsouvenircustomorder.model.RefundStatus;
import org.example.catholicsouvenircustomorder.service.ComplaintService;
import org.example.catholicsouvenircustomorder.service.RefundService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for Admin complaint operations
 * Handles viewing all complaints, approving/rejecting, and managing refund transactions
 * Requirements: 3.1-3.7, 9.1, 9.2
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {
    
    private final ComplaintService complaintService;
    private final RefundService refundService;
    
    /**
     * Get all complaints with optional status filter and pagination
     * GET /api/admin/complaints?status={status}&page={page}&size={size}
     * Requirements: 3.1
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<Page<ComplaintResponse>>> getAllComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("Admin {} fetching complaints, status: {}, page: {}, size: {}", 
                adminId, status, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ComplaintResponse> response = complaintService.getAllComplaints(status, pageable);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy danh sách đơn khiếu nại thành công",
                response
        ));
    }
    
    /**
     * Get complaint details
     * GET /api/admin/complaints/{id}
     * Requirements: 3.1
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<ComplaintDetailResponse>> getComplaintDetails(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("Admin {} fetching complaint details: {}", adminId, id);
        
        ComplaintDetailResponse response = complaintService.getComplaintDetails(id, adminId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy chi tiết đơn khiếu nại thành công",
                response
        ));
    }
    
    /**
     * Approve complaint
     * POST /api/admin/complaints/{id}/approve
     * Requirements: 3.1-3.7
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<ComplaintResponse>> approveComplaint(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveComplaintRequest request,
            Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("Admin {} approving complaint: {}, refundAmount: {}", 
                adminId, id, request.getRefundAmount());
        
        ComplaintResponse response = complaintService.approveComplaint(id, request, adminId);
        
        String message = "Phê duyệt đơn khiếu nại thành công. Hệ thống đã xử lý hoàn tiền.";
        
        return ResponseEntity.ok(BaseResponse.success(message, response));
    }
    
    /**
     * Reject complaint
     * POST /api/admin/complaints/{id}/reject
     * Requirements: 3.6, 3.7
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<ComplaintResponse>> rejectComplaint(
            @PathVariable UUID id,
            @Valid @RequestBody RejectComplaintRequest request,
            Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("Admin {} rejecting complaint: {}", adminId, id);
        
        ComplaintResponse response = complaintService.rejectComplaint(id, request, adminId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Từ chối đơn khiếu nại thành công. Khách hàng đã được thông báo.",
                response
        ));
    }
    
    /**
     * Get all refund transactions with optional status filter and pagination
     * GET /api/admin/complaints/refund-transactions?status={status}&page={page}&size={size}
     * Requirements: 9.1, 9.2
     */
    @GetMapping("/refund-transactions")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<Page<RefundTransactionResponse>>> getAllRefundTransactions(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("Admin {} fetching refund transactions, status: {}, page: {}, size: {}", 
                adminId, status, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<RefundTransactionResponse> response = refundService.getAllRefundTransactions(status, pageable);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy danh sách giao dịch hoàn tiền thành công",
                response
        ));
    }
    
    /**
     * Retry failed refund
     * POST /api/admin/complaints/refund-transactions/{id}/retry
     * Requirements: 11.5
     */
    @PostMapping("/refund-transactions/{id}/retry")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<RefundTransactionResponse>> retryRefund(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID adminId = (UUID) authentication.getPrincipal();
        log.info("Admin {} retrying refund transaction: {}", adminId, id);
        
        refundService.retryRefund(id, adminId);
        RefundTransactionResponse response = refundService.getRefundTransaction(id);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Thử lại hoàn tiền thành công",
                response
        ));
    }
}
