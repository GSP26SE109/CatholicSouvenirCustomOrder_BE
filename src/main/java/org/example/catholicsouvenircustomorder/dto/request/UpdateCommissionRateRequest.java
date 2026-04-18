package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating commission rate
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommissionRateRequest {
    
    /**
     * New commission rate percentage (must be between 0 and 100)
     */
    @NotNull(message = "Commission rate không được để trống")
    @DecimalMin(value = "0.00", message = "Commission rate phải >= 0")
    @DecimalMax(value = "100.00", message = "Commission rate phải <= 100")
    private BigDecimal commissionRate;
}
