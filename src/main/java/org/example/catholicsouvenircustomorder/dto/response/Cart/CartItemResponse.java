package org.example.catholicsouvenircustomorder.dto.response.Cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CartItemResponse {
    private UUID productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private List<String> images;
}
