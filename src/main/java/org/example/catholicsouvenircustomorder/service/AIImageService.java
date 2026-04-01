package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.AIPromptRequest;
import org.example.catholicsouvenircustomorder.dto.response.AIImageResponse;
import org.example.catholicsouvenircustomorder.model.ProductTemplate;

import java.util.Map;

public interface AIImageService {
    String generateImage(String prompt);
    
    /**
     * Build AI prompt from template and zone inputs
     */
    String buildPromptFromTemplate(ProductTemplate template, Map<String, String> zoneInputs, String additionalDescription);
    
    /**
     * Generate concept image from prompt request
     */
    AIImageResponse generateConceptImage(AIPromptRequest request);
}
