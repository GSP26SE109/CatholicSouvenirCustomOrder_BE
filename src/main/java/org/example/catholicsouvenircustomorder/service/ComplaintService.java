package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.Complaint.ApproveComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.ArtisanResponseRequest;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.CreateComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.RejectComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintResponse;
import org.example.catholicsouvenircustomorder.model.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for complaint management
 * Handles complaint creation, artisan response, admin review, and queries
 */
public interface ComplaintService {
    
    /**
     * Create a new complaint for an order
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 10.1, 10.2, 10.3, 10.4, 11.2
     */
    ComplaintResponse createComplaint(CreateComplaintRequest request, UUID customerId);
    
    /**
     * Artisan responds to complaint and decides on return requirement
     * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8
     */
    ComplaintResponse respondToComplaint(UUID complaintId, ArtisanResponseRequest request, UUID artisanId);
    
    /**
     * Admin approves complaint
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.7, 4.1, 4.2, 6.1, 6.2, 6.3
     */
    ComplaintResponse approveComplaint(UUID complaintId, ApproveComplaintRequest request, UUID adminId);
    
    /**
     * Admin rejects complaint
     * Requirements: 3.6, 3.7, 6.3
     */
    ComplaintResponse rejectComplaint(UUID complaintId, RejectComplaintRequest request, UUID adminId);
    
    /**
     * Get complaint details with authorization check
     * Requirements: 7.1, 7.2
     */
    ComplaintDetailResponse getComplaintDetails(UUID complaintId, UUID userId);
    
    /**
     * List complaints for customer with pagination
     * Requirements: 7.1, 7.2
     */
    Page<ComplaintResponse> getCustomerComplaints(UUID customerId, Pageable pageable);
    
    /**
     * List complaints for artisan with pagination
     * Requirements: 8.1, 8.2
     */
    Page<ComplaintResponse> getArtisanComplaints(UUID artisanId, Pageable pageable);
    
    /**
     * List all complaints for admin with filter by status
     * Requirements: 3.1
     */
    Page<ComplaintResponse> getAllComplaints(ComplaintStatus status, Pageable pageable);
    
    /**
     * Validate if order is eligible for complaint
     * Requirements: 1.4, 1.5
     */
    boolean isEligibleForComplaint(UUID orderId, UUID customOrderId);
}
