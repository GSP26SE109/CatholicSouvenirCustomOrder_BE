package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.ImageValidationResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AIImageValidationService {
    /**
     * Validate if image contains Catholic religious items
     * @param image Image file to validate
     * @return Validation result with confidence score and details
     */
    ImageValidationResponse validateCatholicImage(MultipartFile image);
    
    /**
     * Validate image by URL
     * @param imageUrl URL of the image to validate
     * @return Validation result with confidence score and details
     */
    ImageValidationResponse validateCatholicImageByUrl(String imageUrl);
}
