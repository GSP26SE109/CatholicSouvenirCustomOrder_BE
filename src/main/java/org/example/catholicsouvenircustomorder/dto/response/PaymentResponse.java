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

/**
 * Response DTO for order payments (Template-Based Flow & Product Orders)
 * Used for Payment entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private UUID paymentId;
    
    // Order reference
    private UUID orderId;
    
    // Payment details
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    
    // Transaction details
    private String transactionId;
    private String paymentUrl;
    private String failureReason;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
