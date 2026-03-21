package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.Complaint.ComplaintRequest;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.ComplaintResponse;
import org.example.catholicsouvenircustomorder.exception.InvalidCredentialsException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Complaint;
import org.example.catholicsouvenircustomorder.model.ComplaintEvidence;
import org.example.catholicsouvenircustomorder.model.ComplaintStatus;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.repository.ComplaintEvidenceRepository;
import org.example.catholicsouvenircustomorder.repository.ComplaintRepository;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.service.ComplaintService;
import org.example.catholicsouvenircustomorder.util.ImageHelper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImp implements ComplaintService {
    private final OrderRepository orderRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintEvidenceRepository complaintEvidenceRepository;
    private final ImageHelper imageHelper;

    @Override
    public ComplaintResponse createComplaint(UUID customerId, ComplaintRequest complaintRequest) {
        Order order = orderRepository.findById(complaintRequest.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order không tồn tại"));

        UUID artisanId = order.getOrderDetails().stream()
                .map(detail -> detail.getProduct().getArtisan().getArtisanUuid())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy artisan cho order này"));

        Complaint complaint = new Complaint();
        complaint.setCustomerId(customerId);
        complaint.setArtisanId(artisanId);
        complaint.setOrder(order);
        complaint.setStatus(ComplaintStatus.UNDER_REVIEW);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setType(complaintRequest.getComplaintType());
        complaint.setRefundAmount(BigDecimal.ZERO);

        Complaint savedComplaint = complaintRepository.save(complaint);

        List<String> uploadedUrls = imageHelper.uploadImage(complaintRequest.getImages());

        ComplaintEvidence complaintEvidence = new ComplaintEvidence();
        complaintEvidence.setComplaint(savedComplaint);
        complaintEvidence.setImageUrl(uploadedUrls);
        complaintEvidence.setMessage(complaintRequest.getMessage());
        complaintEvidence.setUploadedBy(customerId);
        complaintEvidence.setCreatedAt(LocalDateTime.now());
        complaintEvidenceRepository.save(complaintEvidence);

        return ComplaintResponse.builder()
                .complaintId(savedComplaint.getComplaintId())
                .orderId(order.getOrderId())
                .customerId(order.getCustomer().getAccountId())
                .artisanId(artisanId)
                .status(String.valueOf(savedComplaint.getStatus()))
                .type(String.valueOf(savedComplaint.getType()))
                .refundAmount(savedComplaint.getRefundAmount())
                .build();
    }

    @Override
    public ComplaintResponse artisanResponse(UUID artisanId, UUID complaintId, ComplaintRequest complaintRequest) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint không tồn tại"));

        boolean isOwner = complaint.getOrder().getOrderDetails().stream()
                .anyMatch(detail -> detail.getProduct().getArtisan().getArtisanUuid().equals(artisanId));
        if (!isOwner) {
            throw new InvalidCredentialsException("Artisan không có quyền phản hồi complaint này");
        }

        List<String> newUploadedUrls = imageHelper.uploadImage(complaintRequest.getImages());

        ComplaintEvidence artisanEvidence = complaint.getEvidences().stream()
                .filter(e -> e.getUploadedBy().equals(artisanId))
                .findFirst()
                .orElse(null);

        if (artisanEvidence != null) {
            artisanEvidence.getImageUrl().addAll(newUploadedUrls);
            artisanEvidence.setMessage(complaintRequest.getMessage());
        } else {
            artisanEvidence = new ComplaintEvidence();
            artisanEvidence.setComplaint(complaint);
            artisanEvidence.setImageUrl(new ArrayList<>(newUploadedUrls));
            artisanEvidence.setMessage(complaintRequest.getMessage());
            artisanEvidence.setUploadedBy(artisanId);
            artisanEvidence.setCreatedAt(LocalDateTime.now());
            complaint.getEvidences().add(artisanEvidence);
        }

        complaintRepository.save(complaint);

        return ComplaintResponse.builder()
                .complaintId(complaint.getComplaintId())
                .orderId(complaint.getOrder().getOrderId())
                .customerId(complaint.getCustomerId())
                .artisanId(complaint.getArtisanId())
                .status(String.valueOf(complaint.getStatus()))
                .type(String.valueOf(complaint.getType()))
                .refundAmount(complaint.getRefundAmount())
                .build();
    }

    @Override
    public ComplaintResponse adminResolve(ComplaintStatus status, BigDecimal refundAmount) {
        return null;
    }
}
