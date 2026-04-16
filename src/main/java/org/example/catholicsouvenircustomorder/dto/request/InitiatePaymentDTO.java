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
     * URL to redirect after payment (success or failed).
     * - Web: https://yourweb.com/payment/result
     * - Mobile: yourapp://payment/result
     * If null, uses default from config
     * 
     * Note: Payment status update is handled by IPN callback (/vnpay/ipn, /zalopay/ipn)
     * This URL is only for user redirection back to app
     */
    private String returnUrl;
}
