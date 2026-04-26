package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.CartItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    
    private UUID cartItemId;
    private CartItemType type;
    
    // Product info (if type = PRODUCT)
    private UUID productId;
    private String productName;
    private String productImage;
    private Integer availableStock; // NEW: Available stock for product
    private Boolean isAvailable;    // NEW: Is product still available
    
    // Template info (if type = TEMPLATE)
    private UUID templateId;
    private String templateName;
    private String templateImage;
    private Map<String, String> customizationData;
    
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private LocalDateTime addedAt;
}
