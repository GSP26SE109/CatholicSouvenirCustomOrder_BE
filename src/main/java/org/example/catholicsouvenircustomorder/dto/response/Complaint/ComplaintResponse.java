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
 * Basic complaint response DTO
 * Requirements: 7.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponse {
    private UUID complaintId;
    private UUID orderId;
    private UUID customOrderId;
    private UUID customerId;
    private String customerName;
    private UUID artisanId;
    private String artisanName;
    private String reason;
    private ComplaintStatus status;
    private Boolean requireReturn;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

