package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.CartItem;
import org.example.catholicsouvenircustomorder.model.CartItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    
    List<CartItem> findByCart_CartId(UUID cartId);
    
    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(UUID cartId, UUID productId);
    
    Optional<CartItem> findByCart_CartIdAndTemplate_TemplateId(UUID cartId, UUID templateId);
    
    List<CartItem> findByCart_CartIdAndType(UUID cartId, CartItemType type);
    
    void deleteByCart_CartId(UUID cartId);
}
