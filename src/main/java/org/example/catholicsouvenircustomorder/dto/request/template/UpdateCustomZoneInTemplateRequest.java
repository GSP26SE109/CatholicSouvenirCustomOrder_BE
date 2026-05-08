package org.example.catholicsouvenircustomorder.dto.request.template;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.InputType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class UpdateCustomZoneInTemplateRequest {
    
    // If zoneId is provided, update existing zone
    // If zoneId is null, create new zone
    private UUID zoneId;
    
    @NotBlank(message = "Zone name is required")
    @Size(max = 100, message = "Zone name must not exceed 100 characters")
    private String zoneName;
    
    @Size(max = 500, message = "Zone description must not exceed 500 characters")
    private String zoneDescription;
    
    @NotNull(message = "Input type is required")
    private InputType inputType;
    
    private Map<String, Object> inputConstraints;
    
    @DecimalMin(value = "0.00", message = "Extra price must be non-negative")
    private BigDecimal extraPrice;
    
    @NotNull(message = "Is required field is required")
    private Boolean isRequired;
    
    @NotNull(message = "Sort order is required")
    @Min(value = 0, message = "Sort order must be non-negative")
    private Integer sortOrder;
}
