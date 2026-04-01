package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteStageRequest {
    @NotBlank(message = "Completion image URL is required")
    private String completionImageUrl;
    
    private String notes; // Optional notes from artisan
}
