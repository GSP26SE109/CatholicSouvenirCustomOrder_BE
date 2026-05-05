package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageValidationResponse {
    private boolean isValid;
    private double confidenceScore;
    private String category;
    private List<String> detectedItems;
    private String message;
    private String warning;
    private boolean requiresManualReview;
}
