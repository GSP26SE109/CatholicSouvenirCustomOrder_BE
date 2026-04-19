package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Interface projection for Artisan Custom Order Statistics
 * Used to retrieve custom order performance metrics for artisan dashboard
 */
public interface ArtisanCustomOrderStats {
    Long getTotalRequests();
    Long getTotalOrders();
    BigDecimal getAvgOrderValue();
    Double getConversionRate();
    Long getPendingRequests();
    Double getAvgCompletionDays();
}
