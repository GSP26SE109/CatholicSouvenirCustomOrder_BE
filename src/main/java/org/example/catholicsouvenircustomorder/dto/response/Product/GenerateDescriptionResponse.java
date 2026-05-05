package org.example.catholicsouvenircustomorder.dto.response.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDescriptionResponse {
    private String description;
    private boolean aiGenerated;
    private String message;
}
