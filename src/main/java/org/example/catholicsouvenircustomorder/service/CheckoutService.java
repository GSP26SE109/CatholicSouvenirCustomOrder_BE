package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CalculateShippingRequest;
import org.example.catholicsouvenircustomorder.dto.request.CheckoutRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.CheckoutResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.ShippingFeeResponse;

import java.util.UUID;

/**
 * Service for checkout operations - converting cart to orders
 */
public interface CheckoutService {
    
    /**
     * Checkout cart and create orders (one order per artisan)
     * Supports both product orders and template orders
     * Returns all orders created with total amount
     */
    CheckoutResponseDTO checkout(UUID customerId, CheckoutRequest request);
    
    /**
     * Validate cart before checkout
     */
    void validateCart(UUID customerId);
    
    /**
     * Calculate shipping fee for cart items before checkout
     * Groups items by artisan and calculates separate shipping fees
     */
    ShippingFeeResponse calculateShippingFeeForCart(UUID customerId, CalculateShippingRequest request);
}

