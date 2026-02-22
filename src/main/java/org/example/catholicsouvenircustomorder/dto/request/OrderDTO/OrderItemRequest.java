package org.example.catholicsouvenircustomorder.dto.request.OrderDTO;

import lombok.Data;

@Data
public class OrderItemRequest {
    private int productId;
    private int quantity;
}
