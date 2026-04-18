package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for commission report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionReportResponse {
    
    /**
     * Total commission collected in the period
     */
    private BigDecimal totalCommission;
    
    /**
     * Total number of transactions with commission
     */
    private Long totalTransactions;
    
    /**
     * Average commission per transaction
     */
    private BigDecimal averageCommissionPerTransaction;
    
    /**
     * Detailed items grouped by date/week/month
     */
    private List<CommissionReportItem> items;
}
