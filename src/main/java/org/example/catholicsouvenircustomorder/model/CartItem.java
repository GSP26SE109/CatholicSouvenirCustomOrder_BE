package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Item in shopping cart
 * Can be either a regular product or a template-based customization
 */
@Entity
@Data
@Table(name = "cart_items")
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID cartItemId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartItemType type;
    
    // For PRODUCT type
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    // For TEMPLATE type
    @ManyToOne
    @JoinColumn(name = "template_id")
    private ProductTemplate template;
    
    // Customization data for template (JSON format)
    @Column(columnDefinition = "TEXT")
    private String customizationData;
    
    @Column(nullable = false)
    private Integer quantity = 1;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();
    
    // Helper methods
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
    
    public boolean isProduct() {
        return type == CartItemType.PRODUCT;
    }
    
    public boolean isTemplate() {
        return type == CartItemType.TEMPLATE;
    }
}
