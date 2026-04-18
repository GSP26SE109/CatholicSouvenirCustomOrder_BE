package org.example.catholicsouvenircustomorder.dto.response.Complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.ComplaintStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Detailed complaint response DTO with full information
 * Requirements: 7.3, 7.4, 7.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintDetailResponse {
    // Basic info
    private UUID complaintId;
    private UUID orderId;
    private UUID customOrderId;
    private ComplaintStatus status;
    
    // Customer info
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    
    // Artisan info
    private UUID artisanId;
    private String artisanName;
    
    // Complaint details
    private String reason;
    @Builder.Default
    private List<String> evidenceImages = new ArrayList<>();
    
    // Artisan response (Requirements: 7.3)
    private String artisanResponse;
    private Boolean requireReturn;
    private LocalDateTime artisanResponseAt;
    
    // Admin decision (Requirements: 7.4)
    private BigDecimal refundAmount;
    private String adminNote;
    private String rejectionReason;
    private UUID reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    
    // Timestamps (Requirements: 7.5)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Refund transaction info
    private UUID refundTransactionId;
    private String refundStatus;
}
