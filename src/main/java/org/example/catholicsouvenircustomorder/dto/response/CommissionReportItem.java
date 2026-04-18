package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Individual item in commission report
 * Represents commission data for a specific date/week/month
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionReportItem {
    
    /**
     * Date for this report item
     */
    private LocalDate date;
    
    /**
     * Total commission for this period
     */
    private BigDecimal totalCommission;
    
    /**
     * Number of transactions in this period
     */
    private Long transactionCount;
}
