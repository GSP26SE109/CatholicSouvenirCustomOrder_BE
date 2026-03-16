package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.Cart.CartResponse;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CartService {
    void addToCart(UUID accountId, UUID productId, int quantity) ;
    CartResponse getCart(UUID accountId);
    void clearCart(UUID accountId);
    void removeFromCart(UUID accountId, UUID productId);
    void updateCart(UUID accountId, UUID productId, int quantity);
    OrderResponseDTO checkout(UUID accountId);
}
