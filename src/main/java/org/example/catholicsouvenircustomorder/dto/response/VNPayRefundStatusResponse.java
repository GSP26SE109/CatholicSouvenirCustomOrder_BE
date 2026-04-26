package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for VNPay refund status query response
 * Used to check the current status of a refund transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayRefundStatusResponse {
    
    /**
     * VNPay refund transaction ID
     */
    private String vnpayRefundId;
    
    /**
     * VNPay transaction number
     */
    private String vnpayTransactionNo;
    
    /**
     * Current status code
     * "00" = Completed successfully
     * "01" = Processing
     * "02" = Failed
     */
    private String statusCode;
    
    /**
     * Status message
     */
    private String statusMessage;
    
    /**
     * Refund amount in VND
     */
    private BigDecimal refundAmount;
    
    /**
     * Original transaction reference
     */
    private String originalTxnRef;
    
    /**
     * Date and time of last status update
     */
    private LocalDateTime lastUpdated;
    
    /**
     * Response code from VNPay API
     */
    private String responseCode;
}
