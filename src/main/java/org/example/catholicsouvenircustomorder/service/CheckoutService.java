package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CheckoutRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;

import java.util.UUID;

/**
 * Service for checkout operations - converting cart to order
 */
public interface CheckoutService {
    
    /**
     * Checkout cart and create order
     * Supports both product orders and template orders
     */
    OrderResponseDTO checkout(UUID customerId, CheckoutRequest request);
    
    /**
     * Validate cart before checkout
     */
    void validateCart(UUID customerId);
}
