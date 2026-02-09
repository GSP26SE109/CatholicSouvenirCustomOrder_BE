package org.example.catholicsouvenircustomorder.dto.response.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDTO {
    private UUID id;
    private int quantity;
    private double unitPrice;
    private double subTotal;
    private double discount;

    private int productId;
}
