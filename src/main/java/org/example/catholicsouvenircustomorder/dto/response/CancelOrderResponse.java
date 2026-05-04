package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for order cancellation
 * Requirements: 10.3, 10.4, 10.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderResponse {
    
    private UUID refundTransactionId;
    private BigDecimal grossRefundAmount;
    private BigDecimal platformCommission;
    private BigDecimal netRefundAmount;
    private List<StageRefundCalculation> stageBreakdown;
    private String status;
}
