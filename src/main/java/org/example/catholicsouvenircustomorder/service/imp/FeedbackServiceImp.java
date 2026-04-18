package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.CreateFeedbackRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateFeedbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.ArtisanRatingResponse;
import org.example.catholicsouvenircustomorder.dto.response.FeedbackResponse;
import org.example.catholicsouvenircustomorder.exception.NotFoundException;
import org.example.catholicsouvenircustomorder.exception.UnauthorizedException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of FeedbackService
 * Handles customer feedback/review operations for completed orders
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackServiceImp implements FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    
    /**
     * Create a new feedback for a completed order
     */
    @Override
    @Transactional
    public FeedbackResponse createFeedback(CreateFeedbackRequest request, UUID customerId) {
        log.info("Creating feedback for customer: {}, orderId: {}, customOrderId: {}", 
                 customerId, request.getOrderId(), request.getCustomOrderId());
        
        // 1. Validate that either orderId or customOrderId is provided
        if (request.getOrderId() == null && request.getCustomOrderId() == null) {
            throw new IllegalArgumentException("Phải cung cấp orderId hoặc customOrderId");
        }
        
        Order order = null;
        CustomOrder customOrder = null;
        Artisan artisan = null;
        
        // 2. Validate order exists and belongs to customer
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("Đơn hàng không tồn tại"));
            
            if (!order.getCustomer().getAccountId().equals(customerId)) {
                throw new UnauthorizedException("Đơn hàng không thuộc về khách hàng này");
            }
            
            // Check order status is DELIVERED
            if (!"DELIVERED".equals(order.getStatus())) {
                throw new IllegalStateException("Chỉ có thể đánh giá đơn hàng đã giao");
            }
            
            // Check if feedback already exists
            if (feedbackRepository.existsByOrderAndCustomer(request.getOrderId(), customerId)) {
                throw new IllegalArgumentException("Bạn đã đánh giá đơn hàng này rồi");
            }
            
            // Determine artisan from order
            artisan = determineArtisanFromOrder(order);
            
        } else if (request.getCustomOrderId() != null) {
            customOrder = customOrderRepository.findById(request.getCustomOrderId())
                .orElseThrow(() -> new NotFoundException("Đơn hàng tùy chỉnh không tồn tại"));
            
            if (!customOrder.getRequest().getCustomer().getAccountId().equals(customerId)) {
                throw new UnauthorizedException("Đơn hàng tùy chỉnh không thuộc về khách hàng này");
            }
            
            // Check custom order status is DELIVERED or COMPLETED
            if (customOrder.getStatus() != CustomOrderStatus.DELIVERED && 
                customOrder.getStatus() != CustomOrderStatus.COMPLETED) {
                throw new IllegalStateException("Chỉ có thể đánh giá đơn hàng đã giao hoặc hoàn thành");
            }
            
            // Check if feedback already exists
            if (feedbackRepository.existsByCustomOrderAndCustomer(request.getCustomOrderId(), customerId)) {
                throw new IllegalArgumentException("Bạn đã đánh giá đơn hàng này rồi");
            }
            
            artisan = customOrder.getArtisan();
        }
        
        // 3. Get customer account
        Account customer = accountRepository.findById(customerId)
            .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại"));
        
        // 4. Create feedback
        Feedback feedback = new Feedback();
        feedback.setOrder(order);
        feedback.setCustomOrder(customOrder);
        feedback.setCustomer(customer);
        feedback.setArtisan(artisan);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setCreatedAt(LocalDateTime.now());
        
        feedback = feedbackRepository.save(feedback);
        
        log.info("Feedback created successfully: {}", feedback.getFeedbackId());
        
        return mapToResponse(feedback);
    }
    
    /**
     * Update an existing feedback
     */
    @Override
    @Transactional
    public FeedbackResponse updateFeedback(UUID feedbackId, UpdateFeedbackRequest request, UUID customerId) {
        log.info("Updating feedback: {} by customer: {}", feedbackId, customerId);
        
        // 1. Get feedback and validate ownership
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new NotFoundException("Đánh giá không tồn tại"));
        
        if (!feedback.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedException("Bạn không có quyền cập nhật đánh giá này");
        }
        
        // 2. Update feedback
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        
        feedback = feedbackRepository.save(feedback);
        
        log.info("Feedback updated successfully: {}", feedbackId);
        
        return mapToResponse(feedback);
    }
    
    /**
     * Delete a feedback
     */
    @Override
    @Transactional
    public void deleteFeedback(UUID feedbackId, UUID customerId) {
        log.info("Deleting feedback: {} by customer: {}", feedbackId, customerId);
        
        // 1. Get feedback and validate ownership
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new NotFoundException("Đánh giá không tồn tại"));
        
        if (!feedback.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedException("Bạn không có quyền xóa đánh giá này");
        }
        
        // 2. Delete feedback
        feedbackRepository.delete(feedback);
        
        log.info("Feedback deleted successfully: {}", feedbackId);
    }
    
    /**
     * Get feedback details by ID
     */
    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(UUID feedbackId) {
        log.info("Getting feedback details: {}", feedbackId);
        
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new NotFoundException("Đánh giá không tồn tại"));
        
        return mapToResponse(feedback);
    }
    
    /**
     * Get all feedbacks by customer with pagination
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getCustomerFeedbacks(UUID customerId, Pageable pageable) {
        log.info("Getting feedbacks for customer: {}", customerId);
        
        Account customer = accountRepository.findById(customerId)
            .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại"));
        
        Page<Feedback> feedbacks = feedbackRepository.findByCustomer(customer, pageable);
        return feedbacks.map(this::mapToResponse);
    }
    
    /**
     * Get all feedbacks for an artisan with pagination
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getArtisanFeedbacks(UUID artisanId, Pageable pageable) {
        log.info("Getting feedbacks for artisan: {}", artisanId);
        
        Artisan artisan = artisanRepository.findById(artisanId)
            .orElseThrow(() -> new NotFoundException("Nghệ nhân không tồn tại"));
        
        Page<Feedback> feedbacks = feedbackRepository.findByArtisan(artisan, pageable);
        return feedbacks.map(this::mapToResponse);
    }
    
    /**
     * Get artisan's average rating and total feedback count
     */
    @Override
    @Transactional(readOnly = true)
    public ArtisanRatingResponse getArtisanRating(UUID artisanId) {
        log.info("Getting rating for artisan: {}", artisanId);
        
        Artisan artisan = artisanRepository.findById(artisanId)
            .orElseThrow(() -> new NotFoundException("Nghệ nhân không tồn tại"));
        
        Double averageRating = feedbackRepository.calculateAverageRatingByArtisan(artisanId);
        Long totalFeedbacks = feedbackRepository.countByArtisan(artisan);
        
        return ArtisanRatingResponse.builder()
            .averageRating(averageRating != null ? averageRating : 0.0)
            .totalFeedbacks(totalFeedbacks)
            .build();
    }
    
    /**
     * Check if customer can review an order
     */
    @Override
    @Transactional(readOnly = true)
    public boolean canReviewOrder(UUID orderId, UUID customOrderId, UUID customerId) {
        if (orderId != null) {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) return false;
            
            // Check ownership
            if (!order.getCustomer().getAccountId().equals(customerId)) return false;
            
            // Check status is DELIVERED
            if (!"DELIVERED".equals(order.getStatus())) return false;
            
            // Check no existing feedback
            return !feedbackRepository.existsByOrderAndCustomer(orderId, customerId);
            
        } else if (customOrderId != null) {
            CustomOrder customOrder = customOrderRepository.findById(customOrderId).orElse(null);
            if (customOrder == null) return false;
            
            // Check ownership
            if (!customOrder.getRequest().getCustomer().getAccountId().equals(customerId)) return false;
            
            // Check status is DELIVERED or COMPLETED
            if (customOrder.getStatus() != CustomOrderStatus.DELIVERED && 
                customOrder.getStatus() != CustomOrderStatus.COMPLETED) return false;
            
            // Check no existing feedback
            return !feedbackRepository.existsByCustomOrderAndCustomer(customOrderId, customerId);
        }
        
        return false;
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Determine artisan from order
     */
    private Artisan determineArtisanFromOrder(Order order) {
        // Try to get artisan from product-based order details
        Artisan artisan = artisanRepository.findByOrderIdFromProduct(order.getOrderId()).orElse(null);
        
        // If not found, try template-based order details
        if (artisan == null) {
            artisan = artisanRepository.findByOrderIdFromTemplate(order.getOrderId()).orElse(null);
        }
        
        if (artisan == null) {
            throw new NotFoundException("Không tìm thấy nghệ nhân cho đơn hàng này");
        }
        
        return artisan;
    }
    
    /**
     * Map Feedback entity to FeedbackResponse DTO
     */
    private FeedbackResponse mapToResponse(Feedback feedback) {
        return FeedbackResponse.builder()
            .feedbackId(feedback.getFeedbackId())
            .orderId(feedback.getOrder() != null ? feedback.getOrder().getOrderId() : null)
            .customOrderId(feedback.getCustomOrder() != null ? feedback.getCustomOrder().getCustomOrderId() : null)
            .customerId(feedback.getCustomer().getAccountId())
            .customerName(feedback.getCustomer().getFullName())
            .customerAvatar(feedback.getCustomer().getAvt_url())
            .artisanId(feedback.getArtisan().getArtisanUuid())
            .artisanName(feedback.getArtisan().getArtisanName())
            .rating(feedback.getRating())
            .comment(feedback.getComment())
            .createdAt(feedback.getCreatedAt())
            .build();
    }
}
