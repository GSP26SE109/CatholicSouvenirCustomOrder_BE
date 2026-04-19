package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

/**
 * Interface projection for top artisan data
 * Used by Spring Data JPA to map query results efficiently
 */
public interface TopArtisanDTO {
    String getArtisanId();
    String getArtisanName();
    Long getTotalOrders();
    BigDecimal getTotalRevenue();
    Double getAverageRating();
}
