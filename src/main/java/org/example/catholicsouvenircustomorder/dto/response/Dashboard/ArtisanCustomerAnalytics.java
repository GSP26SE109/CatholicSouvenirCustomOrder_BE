package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

/**
 * Interface projection for Artisan Customer Analytics
 * Used to retrieve customer-related metrics for artisan dashboard
 */
public interface ArtisanCustomerAnalytics {
    Long getTotalCustomers();
    Double getRepeatCustomerRate();
    Double getCustomerSatisfactionScore();
}
