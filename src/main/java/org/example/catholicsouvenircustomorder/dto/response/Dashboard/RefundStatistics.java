package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Refund and cancellation statistics
 */
public interface RefundStatistics {
    Long getTotalRefunds();
    BigDecimal getTotalRefundAmount();
    Long getTotalCancellations();
    BigDecimal getTotalCancellationAmount();
    Double getRefundRate(); // Percentage of orders that got refunded
}
