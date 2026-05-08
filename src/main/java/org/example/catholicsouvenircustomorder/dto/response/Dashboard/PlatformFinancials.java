package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Platform financial metrics (Admin only)
 * Simplified to show only total platform revenue from admin wallet
 */
public interface PlatformFinancials {
    BigDecimal getTotalPlatformRevenue(); // Total balance in admin wallet (accumulated platform revenue)
}
