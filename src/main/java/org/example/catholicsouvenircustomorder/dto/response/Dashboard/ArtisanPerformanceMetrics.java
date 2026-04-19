package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

/**
 * Interface projection for Artisan Performance Metrics
 * Used to retrieve performance indicators for artisan dashboard
 */
public interface ArtisanPerformanceMetrics {
    Double getAvgRating();
    Long getTotalReviews();
    Double getAvgResponseTimeHours();
    Double getOrderFulfillmentRate();
    Double getComplaintRate();
    Double getOnTimeDeliveryRate();
}
