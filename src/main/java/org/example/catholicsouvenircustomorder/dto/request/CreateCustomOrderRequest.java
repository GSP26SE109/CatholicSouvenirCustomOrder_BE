package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating CustomOrder with stages (Request-Based flow only)
 */
@Data
public class CreateCustomOrderRequest {
    
    @NotNull(message = "Request ID không được để trống")
    private UUID requestId;
    
    @NotNull(message = "Tổng giá không được để trống")
    @DecimalMin(value = "0.01", message = "Tổng giá phải lớn hơn 0")
    private BigDecimal totalPrice;
    
    @NotEmpty(message = "Phải có ít nhất 1 giai đoạn")
    @Size(min = 1, max = 10, message = "Số giai đoạn phải từ 1 đến 10")
    @Valid
    private List<StageDefinition> stages;
    
    private String shippingAddress;
    
    @Data
    public static class StageDefinition {
        
        @NotBlank(message = "Tên giai đoạn không được để trống")
        @Size(max = 100, message = "Tên giai đoạn không được quá 100 ký tự")
        private String name;
        
        @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
        private String description;
        
        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        private BigDecimal amount;
        
        @NotNull(message = "Phần trăm thanh toán không được để trống")
        @Min(value = 1, message = "Phần trăm phải từ 1-100")
        @Max(value = 100, message = "Phần trăm phải từ 1-100")
        private Integer paymentPercentage;
    }
}
