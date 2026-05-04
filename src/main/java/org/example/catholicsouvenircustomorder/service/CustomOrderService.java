package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateOrderWithStagesDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.model.CustomOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for CustomOrder operations.
 * Handles custom order creation, status management, and queries.
 */
public interface CustomOrderService {
    
    /**
     * Create order from negotiation (Request-Based flow)
     * Called by artisan after negotiation with customer
     */
    CustomOrderResponse createFromNegotiation(UUID requestId, UUID artisanId, CreateOrderWithStagesDTO dto);
    
    /**
     * Get customer's orders with pagination
     */
    Page<CustomOrderResponse> getCustomerOrders(UUID customerId, Pageable pageable);
    
    /**
     * Get customer's orders filtered by status
     */
    Page<CustomOrderResponse> getCustomerOrders(UUID customerId, CustomOrderStatus status, Pageable pageable);
    
    /**
     * Get artisan's orders with pagination
     */
    Page<CustomOrderResponse> getArtisanOrders(UUID artisanId, Pageable pageable);
    
    /**
     * Get artisan's orders filtered by status
     */
    Page<CustomOrderResponse> getArtisanOrders(UUID artisanId, CustomOrderStatus status, Pageable pageable);
    
    /**
     * Get order detail by ID
     */
    CustomOrderDetailResponse getOrderDetail(UUID orderId);
    
    /**
     * Update order status
     */
    CustomOrderResponse updateStatus(UUID orderId, CustomOrderStatus status, UUID userId);
    
    /**
     * Cancel order
     */
    CustomOrderResponse cancelOrder(UUID orderId, UUID userId, String reason);
    
    /**
     * Get stages of a custom order
     * Returns list of stages with workflow status (canPay, isPaid, isCompleted)
     */
    List<CustomOrderStageResponse> getOrderStages(UUID orderId);
    
    /**
     * Customer confirms order (Request-Based flow)
     * Transitions order from PENDING_CONFIRMATION to PENDING_PAYMENT
     * Unlocks first stage for payment
     */
    CustomOrderResponse confirmOrder(UUID orderId, UUID customerId);
    
    /**
     * Get custom order by request ID
     * Useful for customer to retrieve order created by artisan from their request
     */
    CustomOrderResponse getOrderByRequestId(UUID requestId);
    
    /**
     * Get refund estimate for order cancellation
     * Calculate refund amount without processing actual cancellation
     */
}
