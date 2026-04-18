package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.ScriptureRecommendRequest;
import org.example.catholicsouvenircustomorder.dto.response.ScriptureRecommendResponse;

/**
 * Service for AI Scripture Recommender
 * Suggests Bible verses or prayers suitable for engraving on Catholic souvenir items
 */
public interface AIScriptureService {
    
    /**
     * Recommend Bible verses based on purpose, product, and theme
     * 
     * @param request Scripture recommendation request with purpose, product, theme
     * @return List of recommended scriptures with explanations
     */
    ScriptureRecommendResponse recommendScriptures(ScriptureRecommendRequest request);
    
    /**
     * Get popular scriptures for a specific occasion
     * 
     * @param occasion e.g., "Baptism", "Wedding", "Christmas", "Confirmation"
     * @param language Language code: "vi", "en", "la"
     * @return List of popular scriptures for the occasion
     */
    ScriptureRecommendResponse getPopularScripturesForOccasion(String occasion, String language);
}
