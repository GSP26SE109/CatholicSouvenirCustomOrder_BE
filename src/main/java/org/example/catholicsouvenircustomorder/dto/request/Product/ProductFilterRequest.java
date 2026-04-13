package org.example.catholicsouvenircustomorder.dto.request.Product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductFilterRequest {
    private String category;
    private List<String> tags;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
