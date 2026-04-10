package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;

import java.util.UUID;

/**
 * Request DTO for initiating Order payment.
 * Used for both Product Order and Template Order (shopping flow via Cart).
 * Customer pays for entire order at once.
 * 
 * @deprecated Use InitiatePaymentDTO instead
 */
@Deprecated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateOrderPaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    private String returnUrl;
    private String cancelUrl;
}
