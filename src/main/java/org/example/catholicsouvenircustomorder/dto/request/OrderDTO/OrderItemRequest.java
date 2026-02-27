package org.example.catholicsouvenircustomorder.dto.request.OrderDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class OrderItemRequest {
    @NotNull(message = "Product ID is required")
    private UUID productId;
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}
