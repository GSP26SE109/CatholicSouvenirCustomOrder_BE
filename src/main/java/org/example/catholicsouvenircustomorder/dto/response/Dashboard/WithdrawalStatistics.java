package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Withdrawal request statistics
 */
public interface WithdrawalStatistics {
    Long getTotalRequests();
    Long getPendingRequests();
    Long getApprovedRequests();
    Long getRejectedRequests();
    BigDecimal getTotalWithdrawnAmount();
    BigDecimal getPendingWithdrawalAmount();
}
