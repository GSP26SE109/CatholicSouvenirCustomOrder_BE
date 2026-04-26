package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for VNPay refund API response
 * Contains refund transaction details returned from VNPay
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayRefundResponse {
    
    /**
     * VNPay refund transaction ID
     */
    private String vnpayRefundId;
    
    /**
     * VNPay transaction number for the refund
     */
    private String vnpayTransactionNo;
    
    /**
     * Response code from VNPay
     * "00" = Success
     * Other codes indicate specific errors
     */
    private String responseCode;
    
    /**
     * Response message from VNPay
     */
    private String message;
    
    /**
     * Refund amount in VND
     */
    private BigDecimal refundAmount;
    
    /**
     * Date and time when refund was processed
     */
    private LocalDateTime refundDate;
}
