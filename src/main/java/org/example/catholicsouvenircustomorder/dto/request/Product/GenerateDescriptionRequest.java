package org.example.catholicsouvenircustomorder.dto.request.Product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateDescriptionRequest {
    @NotBlank(message = "Product name is required")
    private String productName;
    
    private String category;
    private String tags;
    private String existingDescription;
}
