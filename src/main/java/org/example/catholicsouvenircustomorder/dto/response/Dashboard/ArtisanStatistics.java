package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

/**
 * Interface projection for artisan statistics
 * Used by Spring Data JPA to map query results efficiently
 */
public interface ArtisanStatistics {
    Long getTotalArtisans();
    Long getPendingArtisans();
    Long getActiveArtisans();
}
