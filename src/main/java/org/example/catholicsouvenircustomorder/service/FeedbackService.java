package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateFeedbackRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateFeedbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.ArtisanRatingResponse;
import org.example.catholicsouvenircustomorder.dto.response.FeedbackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for feedback/review management
 * Handles customer reviews and ratings for completed orders
 */
public interface FeedbackService {
    
    /**
     * Create a new feedback for a completed order
     * Customer can only review orders that are DELIVERED or COMPLETED
     */
    FeedbackResponse createFeedback(CreateFeedbackRequest request, UUID customerId);
    
    /**
     * Update an existing feedback
     * Customer can only update their own feedback
     */
    FeedbackResponse updateFeedback(UUID feedbackId, UpdateFeedbackRequest request, UUID customerId);
    
    /**
     * Delete a feedback
     * Customer can only delete their own feedback
     */
    void deleteFeedback(UUID feedbackId, UUID customerId);
    
    /**
     * Get feedback details by ID
     */
    FeedbackResponse getFeedbackById(UUID feedbackId);
    
    /**
     * Get all feedbacks by customer with pagination
     */
    Page<FeedbackResponse> getCustomerFeedbacks(UUID customerId, Pageable pageable);
    
    /**
     * Get all feedbacks for an artisan with pagination
     */
    Page<FeedbackResponse> getArtisanFeedbacks(UUID artisanId, Pageable pageable);
    
    /**
     * Get artisan's average rating and total feedback count
     */
    ArtisanRatingResponse getArtisanRating(UUID artisanId);
    
    /**
     * Check if customer can review an order
     */
    boolean canReviewOrder(UUID orderId, UUID customOrderId, UUID customerId);
}
