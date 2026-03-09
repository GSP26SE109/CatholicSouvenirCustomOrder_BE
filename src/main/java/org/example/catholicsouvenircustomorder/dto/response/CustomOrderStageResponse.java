package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.PaymentStatus;

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
    private PaymentStatus paymentStatus;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private String completionImageUrl;
    private Boolean canPay;
    private Boolean canComplete;
    private LocalDateTime createdAt;
}
