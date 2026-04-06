package org.example.catholicsouvenircustomorder.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class CartItemRequest {
    private UUID productId;
    private int quantity;
}
