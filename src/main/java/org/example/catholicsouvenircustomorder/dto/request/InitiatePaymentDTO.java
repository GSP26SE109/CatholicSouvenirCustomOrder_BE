package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentDTO {
    
    // Only one of these should be provided
    private UUID orderId;           // For regular product orders
    private UUID customOrderId;     // For template-based custom orders
    private UUID stageId;           // For request-based stage payments
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
    
    private String returnUrl;
    private String cancelUrl;
}
