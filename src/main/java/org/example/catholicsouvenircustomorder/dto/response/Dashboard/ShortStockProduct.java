package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.util.UUID;

public interface ShortStockProduct {
    UUID getProductId();
    String getProductName();
    int getQuantity();
}
