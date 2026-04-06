package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for creating a free-form custom request (Request-Based flow).
 * Customer describes their requirements and sets budget range for artisans to bid.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFreeFormRequestDTO {
    
    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 50, max = 5000, message = "Mô tả phải từ 50 đến 5000 ký tự")
    private String description;
    
    @NotNull(message = "Ngân sách tối thiểu không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Ngân sách tối thiểu phải lớn hơn 0")
    private BigDecimal minBudget;
    
    @NotNull(message = "Ngân sách tối đa không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Ngân sách tối đa phải lớn hơn 0")
    private BigDecimal maxBudget;
    
    @Builder.Default
    private List<String> referenceImages = new ArrayList<>();
    
    @Builder.Default
    private Boolean generateAiImage = false;
}
