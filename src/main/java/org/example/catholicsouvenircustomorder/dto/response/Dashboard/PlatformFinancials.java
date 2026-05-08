package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Platform financial metrics (Admin only)
 */
public interface PlatformFinancials {
    BigDecimal getTotalCommissionEarned();
    BigDecimal getTotalLockedBalance(); // Sum of all locked balances across artisans
    BigDecimal getTotalAvailableBalance(); // Sum of all available balances
    BigDecimal getTotalPlatformRevenue(); // Commission - Refunds
}
