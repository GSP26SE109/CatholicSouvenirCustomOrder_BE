package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

/**
 * Interface projection for Artisan Template Performance
 * Used to retrieve template-related metrics for artisan dashboard
 */
public interface ArtisanTemplatePerformance {
    Long getTotalTemplates();
    Double getTemplateConversionRate();
}
