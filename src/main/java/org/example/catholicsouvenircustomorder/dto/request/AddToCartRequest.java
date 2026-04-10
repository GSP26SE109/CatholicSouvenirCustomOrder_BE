package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.CartItemType;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    
    @NotNull(message = "Type không được để trống")
    private CartItemType type;
    
    // For PRODUCT type
    private UUID productId;
    
    // For TEMPLATE type
    private UUID templateId;
    private Map<String, String> customizationData;
    
    @NotNull(message = "Quantity không được để trống")
    @Min(value = 1, message = "Quantity phải lớn hơn 0")
    private Integer quantity;
}
