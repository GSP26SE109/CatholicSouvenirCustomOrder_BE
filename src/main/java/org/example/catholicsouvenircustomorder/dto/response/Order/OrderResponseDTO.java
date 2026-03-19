package org.example.catholicsouvenircustomorder.dto.response.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private UUID orderId;
    private LocalDateTime orderDate;
    private BigDecimal total;
    private String status;
    private String paymentMethod;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    private UUID customerId;
    private String fullName;
    private List<OrderDetailResponseDTO> orderDetails;
}

