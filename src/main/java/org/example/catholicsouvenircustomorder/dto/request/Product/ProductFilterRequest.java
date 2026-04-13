package org.example.catholicsouvenircustomorder.dto.request.Product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductFilterRequest {
    private UUID category;
    private List<String> tags;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
