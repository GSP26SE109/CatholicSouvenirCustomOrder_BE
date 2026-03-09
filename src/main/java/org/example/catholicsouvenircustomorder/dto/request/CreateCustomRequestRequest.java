package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateCustomRequestRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String referenceImageUrl;
    
    private Boolean generateAiImage = true;
    
    @NotEmpty(message = "At least one artisan must be selected")
    private List<UUID> selectedArtisanIds;
}
