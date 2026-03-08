package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(UUID cart_CartId, UUID product_Id);
}
