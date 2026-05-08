package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Refund impact on artisan (Artisan dashboard)
 */
public interface ArtisanRefundImpact {
    Long getTotalRefunds();
    BigDecimal getTotalRefundAmount();
    Long getTotalCancellations();
    BigDecimal getTotalCancellationAmount();
    BigDecimal getLockedBalance();
    BigDecimal getAvailableBalance();
}
