package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Interface projection for product analytics
 * Used by Spring Data JPA to map query results efficiently
 */
public interface ProductAnalytics {
    Long getTotalProducts();
    Long getPendingProducts();
    Long getApprovedProducts();
    BigDecimal getAveragePrice();
}
