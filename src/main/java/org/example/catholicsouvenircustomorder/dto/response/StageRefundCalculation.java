package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for stage-level refund calculation breakdown
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageRefundCalculation {
    
    private UUID stageId;
    private String stageName;
    private BigDecimal paidAmount;
    private BigDecimal refundPercentage;
    private BigDecimal grossRefund;
    private BigDecimal platformCommission;
    private BigDecimal netRefund;
    private String refundReason;
}
