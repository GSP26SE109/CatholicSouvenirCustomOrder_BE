package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a Request-Based custom order with payment stages.
 * Used by artisan after negotiation with customer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderWithStagesDTO {
    
    @NotNull(message = "Request ID không được để trống")
    private UUID requestId;
    
    @NotNull(message = "Tổng giá không được để trống")
    @DecimalMin(value = "0.01", message = "Tổng giá phải lớn hơn 0")
    private BigDecimal totalPrice;
    
    @NotNull(message = "Danh sách giai đoạn không được để trống")
    @NotEmpty(message = "Phải có ít nhất một giai đoạn thanh toán")
    @Valid
    private List<StageDTO> stages;
}
