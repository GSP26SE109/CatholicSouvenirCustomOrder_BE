package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateFeedbackRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateFeedbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.ArtisanRatingResponse;
import org.example.catholicsouvenircustomorder.dto.response.FeedbackResponse;
import org.example.catholicsouvenircustomorder.service.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for Customer feedback/review operations
 * Handles creating, updating, deleting, and viewing reviews for completed orders
 */
@Slf4j
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    
    /**
     * Create a new feedback/review for a completed order
     * POST /api/feedbacks
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<FeedbackResponse>> createFeedback(
            @Valid @RequestBody CreateFeedbackRequest request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} creating feedback for order: {}, customOrder: {}", 
                customerId, request.getOrderId(), request.getCustomOrderId());
        
        FeedbackResponse response = feedbackService.createFeedback(request, customerId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Tạo đánh giá thành công",
                response
        ));
    }
    
    /**
     * Update an existing feedback/review
     * PUT /api/feedbacks/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<FeedbackResponse>> updateFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeedbackRequest request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} updating feedback: {}", customerId, id);
        
        FeedbackResponse response = feedbackService.updateFeedback(id, request, customerId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Cập nhật đánh giá thành công",
                response
        ));
    }
    
    /**
     * Delete a feedback/review
     * DELETE /api/feedbacks/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<Void>> deleteFeedback(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} deleting feedback: {}", customerId, id);
        
        feedbackService.deleteFeedback(id, customerId);
        
        return ResponseEntity.ok(BaseResponse.success("Xóa đánh giá thành công"));
    }
    
    /**
     * Get feedback details by ID
     * GET /api/feedbacks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<FeedbackResponse>> getFeedbackById(@PathVariable UUID id) {
        log.info("Getting feedback details: {}", id);
        
        FeedbackResponse response = feedbackService.getFeedbackById(id);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy chi tiết đánh giá thành công",
                response
        ));
    }
    
    /**
     * Get my feedbacks with pagination
     * GET /api/feedbacks/my?page={page}&size={size}
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<Page<FeedbackResponse>>> getMyFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} fetching feedbacks, page: {}, size: {}", customerId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FeedbackResponse> response = feedbackService.getCustomerFeedbacks(customerId, pageable);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy danh sách đánh giá thành công",
                response
        ));
    }
    
    /**
     * Get feedbacks for an artisan with pagination
     * GET /api/feedbacks/artisan/{artisanId}?page={page}&size={size}
     */
    @GetMapping("/artisan/{artisanId}")
    public ResponseEntity<BaseResponse<Page<FeedbackResponse>>> getArtisanFeedbacks(
            @PathVariable UUID artisanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting feedbacks for artisan: {}, page: {}, size: {}", artisanId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FeedbackResponse> response = feedbackService.getArtisanFeedbacks(artisanId, pageable);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy danh sách đánh giá thành công",
                response
        ));
    }
    
    /**
     * Get artisan's average rating and total feedback count
     * GET /api/feedbacks/artisan/{artisanId}/rating
     */
    @GetMapping("/artisan/{artisanId}/rating")
    public ResponseEntity<BaseResponse<ArtisanRatingResponse>> getArtisanRating(
            @PathVariable UUID artisanId) {
        log.info("Getting rating for artisan: {}", artisanId);
        
        ArtisanRatingResponse response = feedbackService.getArtisanRating(artisanId);
        
        return ResponseEntity.ok(BaseResponse.success(
                "Lấy đánh giá nghệ nhân thành công",
                response
        ));
    }
    
    /**
     * Check if customer can review an order
     * GET /api/feedbacks/can-review?orderId={orderId}&customOrderId={customOrderId}
     */
    @GetMapping("/can-review")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<Boolean>> canReviewOrder(
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) UUID customOrderId,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Checking if customer {} can review order: {}, customOrder: {}", 
                customerId, orderId, customOrderId);
        
        boolean canReview = feedbackService.canReviewOrder(orderId, customOrderId, customerId);
        
        return ResponseEntity.ok(BaseResponse.success(
                canReview ? "Có thể đánh giá" : "Không thể đánh giá",
                canReview
        ));
    }
}
