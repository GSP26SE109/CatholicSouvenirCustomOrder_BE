package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Interface projection for Wallet Balance Trend
 * Used to retrieve daily wallet balance snapshots for artisan dashboard
 */
public interface WalletBalanceTrend {
    LocalDate getDate();
    BigDecimal getBalance();
}
