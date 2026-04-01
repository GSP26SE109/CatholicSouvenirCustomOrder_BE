package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;
import org.example.catholicsouvenircustomorder.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID paymentId;
    
    // Related entity IDs
    private UUID orderId;
    private UUID customOrderId;
    private UUID stageId;
    private String stageName;
    
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId;
    private String paymentUrl;
    private String failureReason;
    
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
