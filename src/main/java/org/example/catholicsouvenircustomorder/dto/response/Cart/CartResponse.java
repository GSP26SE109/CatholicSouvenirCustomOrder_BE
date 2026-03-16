package org.example.catholicsouvenircustomorder.dto.response.Cart;

import lombok.Builder;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.Product;

import java.math.BigDecimal;
import java.util.List;


@Data
@Builder
public class CartResponse {
    private List<CartItemResponse> items;

    BigDecimal totalPrice;
}
