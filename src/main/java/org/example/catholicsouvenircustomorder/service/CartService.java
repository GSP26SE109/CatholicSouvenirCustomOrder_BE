package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.AddToCartRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateCartItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {
    
    CartResponse getCart(UUID customerId);
    
    CartResponse addToCart(UUID customerId, AddToCartRequest request);
    
    CartResponse updateCartItem(UUID customerId, UUID cartItemId, UpdateCartItemRequest request);
    
    CartResponse removeCartItem(UUID customerId, UUID cartItemId);
    
    CartResponse clearCart(UUID customerId);
    
    Integer getCartItemCount(UUID customerId);
    
    /**
     * Invalidate cart cache after checkout
     */
    void invalidateCartCache(UUID customerId);
}
