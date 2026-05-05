package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.Product.GenerateDescriptionResponse;

public interface AIProductDescriptionService {
    /**
     * Generate faith-based product description using AI
     * @param productName Name of the product
     * @param category Category of the product
     * @param tags Tags associated with the product
     * @param existingDescription Optional existing description to enhance
     * @return Generated description
     */
    String generateDescription(String productName, String category, String tags, String existingDescription);
    
    /**
     * Generate faith-based product description with detailed response
     * @param productName Name of the product
     * @param category Category of the product
     * @param tags Tags associated with the product
     * @param existingDescription Optional existing description to enhance
     * @return Detailed response with description and metadata
     */
    GenerateDescriptionResponse generateDescriptionDetailed(String productName, String category, String tags, String existingDescription);
}
