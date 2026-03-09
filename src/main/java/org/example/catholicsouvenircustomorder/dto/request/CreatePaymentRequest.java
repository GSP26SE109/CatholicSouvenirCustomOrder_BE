package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;

import java.util.UUID;

@Data
public class CreatePaymentRequest {
    
    @NotNull(message = "Stage ID is required")
    private UUID stageId;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    private String returnUrl;
    private String cancelUrl;
}
