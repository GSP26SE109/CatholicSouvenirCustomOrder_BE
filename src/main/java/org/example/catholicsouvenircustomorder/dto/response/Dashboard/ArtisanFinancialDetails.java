package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Interface projection for Artisan Financial Details
 * Used to retrieve financial statistics for artisan dashboard
 */
public interface ArtisanFinancialDetails {
    BigDecimal getGrossEarnings();
    BigDecimal getTotalCommission();
    BigDecimal getNetEarnings();
    BigDecimal getPendingWithdrawal();
    BigDecimal getCurrentBalance();
}
