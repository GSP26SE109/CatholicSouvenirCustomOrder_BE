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
 * For CustomOrder stage payments, use InitiateStagePaymentRequest instead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentDTO {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
    
    /**
     * URL to redirect after successful payment.
     * - Web: https://yourweb.com/payment/success
     * - Mobile: yourapp://payment/success
     * If null, uses default from config
     */
    private String returnUrl;
    
    /**
     * URL to redirect after cancelled/failed payment.
     * - Web: https://yourweb.com/payment/cancel
     * - Mobile: yourapp://payment/cancel
     * Currently not used by VNPay/ZaloPay (they use returnUrl for both cases)
     */
    private String cancelUrl;
}
