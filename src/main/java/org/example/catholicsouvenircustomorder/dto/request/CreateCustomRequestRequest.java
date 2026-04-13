package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating CustomRequest (Request-Based flow).
 * Customer describes what they want, optionally generates AI image.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomRequestRequest {
    
    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 10, max = 2000, message = "Mô tả phải từ 10-2000 ký tự")
    private String description;
    
    @Builder.Default
    private Boolean generateAiImage = false;
    
    @DecimalMin(value = "0", message = "Ngân sách tối thiểu phải >= 0")
    private BigDecimal minBudget;
    
    @DecimalMin(value = "0", message = "Ngân sách tối đa phải >= 0")
    private BigDecimal maxBudget;
}
