package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface projection for top customer data
 * Used by Spring Data JPA to map query results efficiently
 */
public interface TopCustomerDTO {
    UUID getCustomerId();
    String getCustomerName();
    String getEmail();
    Long getTotalOrders();
    BigDecimal getTotalSpent();
}
