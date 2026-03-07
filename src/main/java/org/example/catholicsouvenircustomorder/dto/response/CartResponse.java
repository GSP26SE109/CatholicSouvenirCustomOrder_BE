package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.Product;

@Data
@Builder
public class CartResponse {
    private Product product;

    private Integer quantity;
}
