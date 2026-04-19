package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Interface projection for custom order statistics
 * Used by Spring Data JPA to map query results efficiently
 */
public interface CustomOrderStatistics {
    Long getTotalRequests();
    Long getTotalOrders();
    BigDecimal getAverageOrderValue();
    Double getConversionRate();
}
