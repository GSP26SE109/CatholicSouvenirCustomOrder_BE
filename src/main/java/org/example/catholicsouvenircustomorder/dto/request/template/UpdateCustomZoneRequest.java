package org.example.catholicsouvenircustomorder.dto.request.template;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.InputType;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class UpdateCustomZoneRequest {
    
    @Size(max = 100, message = "Zone name must not exceed 100 characters")
    private String zoneName;
    
    @Size(max = 500, message = "Zone description must not exceed 500 characters")
    private String zoneDescription;
    
    private InputType inputType;
    
    private Map<String, Object> inputConstraints;
    
    @DecimalMin(value = "0.0", message = "Extra price must be non-negative")
    private BigDecimal extraPrice;
    
    private Boolean isRequired;
    
    @Min(value = 0, message = "Sort order must be non-negative")
    private Integer sortOrder;
}
