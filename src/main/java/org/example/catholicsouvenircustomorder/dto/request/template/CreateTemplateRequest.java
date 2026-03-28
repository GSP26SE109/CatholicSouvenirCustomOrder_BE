package org.example.catholicsouvenircustomorder.dto.request.template;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateTemplateRequest {
    
    @NotNull(message = "Category ID is required")
    private UUID categoryId;
    
    @NotBlank(message = "Template name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
    private BigDecimal basePrice;
    
    @Size(max = 100, message = "Material must not exceed 100 characters")
    private String material;
    
    @Size(max = 100, message = "Style must not exceed 100 characters")
    private String style;
    
    private String basePromptHint;
    
    private List<String> baseImages;
    
    @NotNull(message = "At least one custom zone is required")
    @Size(min = 1, message = "At least one custom zone is required")
    private List<CreateCustomZoneRequest> customZones;
}
