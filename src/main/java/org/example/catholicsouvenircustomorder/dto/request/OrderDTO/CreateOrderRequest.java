package org.example.catholicsouvenircustomorder.dto.request.OrderDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "Người dùng không được để trống")
    private UUID accountId;
    private String paymentMethod;
    private LocalDateTime orderDate;
    @NotBlank(message = "Cần thêm hàng hoá")
    private List<OrderItemRequest> items;
}
