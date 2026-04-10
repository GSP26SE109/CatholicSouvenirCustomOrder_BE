package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for stage information when creating a Request-Based custom order.
 * Used by artisan to define payment stages after negotiation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageDTO {
    
    @NotBlank(message = "Tên giai đoạn không được để trống")
    @Size(max = 200, message = "Tên giai đoạn không được vượt quá 200 ký tự")
    private String name;
    
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
    
    @NotNull(message = "Phần trăm thanh toán không được để trống")
    @Min(value = 1, message = "Phần trăm thanh toán phải lớn hơn 0")
    @Max(value = 100, message = "Phần trăm thanh toán không được vượt quá 100")
    private Integer paymentPercentage;
    
    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;
    
    @Min(value = 1, message = "Số ngày ước tính phải lớn hơn 0")
    private Integer estimatedDays;
}
