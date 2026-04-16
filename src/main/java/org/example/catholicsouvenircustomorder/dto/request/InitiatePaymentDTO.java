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
    
    @NotNull(message = "Order group ID is required")
    private UUID orderGroupId;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
    
    /**
     * URL to redirect after payment (success or failed).
     * Backend will auto-detect platform from URL scheme:
     * - Web: https://yourweb.com/payment/result
     * - Mobile: catholicsouvenir://payment/result (deep link)
     * 
     * If null, uses default web URL from config
     * 
     * Note: Payment status update is handled by IPN callback (/vnpay/ipn)
     * This URL is only for user redirection back to app
     */
    private String returnUrl;
}
