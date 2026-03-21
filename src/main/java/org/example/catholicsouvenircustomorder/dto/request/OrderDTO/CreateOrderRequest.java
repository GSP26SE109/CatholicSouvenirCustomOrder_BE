package org.example.catholicsouvenircustomorder.dto.request.OrderDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotNull(message = "Người dùng không được để trống")
    private UUID accountId;
    private LocalDateTime orderDate;
    @NotEmpty(message = "Cần thêm hàng hoá")
    @Valid
    private List<OrderItemRequest> items;
}
