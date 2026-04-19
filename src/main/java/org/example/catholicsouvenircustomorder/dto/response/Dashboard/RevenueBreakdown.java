package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Interface projection for revenue breakdown by source
 * Used by Spring Data JPA to map query results efficiently
 */
public interface RevenueBreakdown {
    BigDecimal getProductRevenue();
    BigDecimal getTemplateRevenue();
    BigDecimal getCustomRevenue();
    BigDecimal getTotalCommission();
}
