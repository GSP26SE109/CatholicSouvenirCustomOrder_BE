package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;
import java.util.UUID;
public interface TopProductDTO {
    UUID getProductId();
    String getProductName();
    Long getSold();
    BigDecimal getRevenue(); // int * BigDecimal → BigDecimal
}