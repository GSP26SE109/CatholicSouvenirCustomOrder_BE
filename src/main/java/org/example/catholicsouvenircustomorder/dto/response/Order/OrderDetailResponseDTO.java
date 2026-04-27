package org.example.catholicsouvenircustomorder.dto.response.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDTO {
    private UUID id;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private int discount;

    private UUID productId;
    private String productName;
    private String image;
    
    // Review information (nullable if not reviewed yet)
    private OrderDetailReviewDTO review;
}
