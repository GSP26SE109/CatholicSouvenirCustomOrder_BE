package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for cancellation refund estimate
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationEstimate {
    
    private BigDecimal grossRefundAmount;
    private BigDecimal platformCommission;
    private BigDecimal netRefundAmount;
    private List<StageRefundCalculation> stageBreakdown;
    private Boolean canCancel;
    private Boolean artisanHasSufficientBalance;
}
