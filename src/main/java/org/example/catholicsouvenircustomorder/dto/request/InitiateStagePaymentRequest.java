package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;

/**
 * Request DTO for initiating stage payment (Request-Based CustomOrder flow).
 * Customer pays for each CustomOrderStage sequentially.
 * This is handled by StagePaymentService, not PaymentService.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateStagePaymentRequest {
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    private String returnUrl;
}
