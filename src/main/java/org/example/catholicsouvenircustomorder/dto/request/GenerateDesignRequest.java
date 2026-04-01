package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateDesignRequest {
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String style; // traditional, modern, classic, etc.
    
    private String material; // wood, metal, stone, etc.
    
    private String size; // 30cm, 50cm, etc.
}
