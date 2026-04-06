package org.example.catholicsouvenircustomorder.dto.response.Complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponse {
    private UUID complaintId;
    private UUID orderId;
    private UUID customerId;
    private UUID artisanId;
    private String type;
    private String status;
    private BigDecimal refundAmount;
    private List<EvidenceResponse> customerEvidences = new ArrayList<>();
    private List<EvidenceResponse> artisanProof = new ArrayList<>();
}
