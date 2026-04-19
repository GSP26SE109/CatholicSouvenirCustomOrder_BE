package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

/**
 * Interface projection for customer statistics
 * Used by Spring Data JPA to map query results efficiently
 */
public interface CustomerStatistics {
    Long getTotalCustomers();
    Long getNewCustomers();
    Long getActiveCustomers();
}
