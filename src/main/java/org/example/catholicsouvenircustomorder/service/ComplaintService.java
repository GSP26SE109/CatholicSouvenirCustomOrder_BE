package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.Complaint.ComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintResponse;
import org.example.catholicsouvenircustomorder.model.ComplaintStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface ComplaintService {
    ComplaintResponse createComplaint(UUID customerId, ComplaintRequest complaintRequest);

    ComplaintResponse artisanResponse(UUID artisanId, UUID complaintId, ComplaintRequest complaintRequest);

    ComplaintResponse adminResolve(ComplaintStatus status, BigDecimal refundAmount);
}
