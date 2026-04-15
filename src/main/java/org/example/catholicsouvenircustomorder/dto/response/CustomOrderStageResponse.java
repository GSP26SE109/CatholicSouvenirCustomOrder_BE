package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.StageStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomOrderStageResponse {
    private UUID stageId;
    private UUID orderId;
    private Integer stageOrder;
    private String stageName;
    private String description;
    private BigDecimal amount;
    private Integer percentage;
    private StageStatus status;
    
    // Workflow tracking flags
    private Boolean canPay;        // Can customer pay this stage now?
    private Boolean isPaid;        // Has this stage been paid?
    private Boolean isCompleted;   // Has artisan completed this stage?
    
    // Timestamps
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    
    // Proof of work
    private String completionImageUrl;
    
    // Payment information (if available)
    private String paymentMethod;
    private String transactionId;
    private String paymentUrl;
}
