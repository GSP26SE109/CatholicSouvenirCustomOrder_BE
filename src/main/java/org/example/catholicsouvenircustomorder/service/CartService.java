package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.CartResponse;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.model.Cart;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CartService {
    void addToCart(UUID accountId, UUID productId, int quantity) ;
    public List<CartResponse> getCart(UUID accountId);
    void clearCart(UUID accountId);
    void removeFromCart(UUID accountId, UUID productId);
    void updateCart(UUID accountId, UUID productId, int quantity);
    OrderResponseDTO checkout(UUID accountId);
}
