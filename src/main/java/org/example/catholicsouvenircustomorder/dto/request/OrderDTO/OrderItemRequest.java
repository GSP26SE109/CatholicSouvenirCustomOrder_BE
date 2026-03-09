package org.example.catholicsouvenircustomorder.dto.request.OrderDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    @NotNull(message = "Product ID is required")
    private UUID productId;
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}
