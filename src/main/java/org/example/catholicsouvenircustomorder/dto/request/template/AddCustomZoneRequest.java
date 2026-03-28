package org.example.catholicsouvenircustomorder.dto.request.template;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.InputType;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AddCustomZoneRequest {
    
    @NotBlank(message = "Zone name is required")
    @Size(max = 100, message = "Zone name must not exceed 100 characters")
    private String zoneName;
    
    @Size(max = 500, message = "Zone description must not exceed 500 characters")
    private String zoneDescription;
    
    @NotNull(message = "Input type is required")
    private InputType inputType;
    
    private Map<String, Object> inputConstraints;
    
    @DecimalMin(value = "0.0", message = "Extra price must be non-negative")
    private BigDecimal extraPrice = BigDecimal.ZERO;
    
    @NotNull(message = "isRequired flag is required")
    private Boolean isRequired = false;
    
    @NotNull(message = "Sort order is required")
    @Min(value = 0, message = "Sort order must be non-negative")
    private Integer sortOrder = 0;
}
