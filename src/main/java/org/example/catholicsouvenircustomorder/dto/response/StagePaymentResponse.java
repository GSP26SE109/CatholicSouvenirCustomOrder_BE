package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;
import org.example.catholicsouvenircustomorder.model.PaymentStatus;
import org.example.catholicsouvenircustomorder.model.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for stage payment attempts
 * A stage can have multiple payment attempts (retry, different methods, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StagePaymentResponse {
    
    private UUID paymentId;
    
    // Stage information
    private UUID stageId;
    private String stageName;
    private Integer stageOrder;
    
    // Payment details
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private PaymentType paymentType;
    
    // Transaction details
    private String transactionId;
    private String paymentUrl;
    private String failureReason;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
