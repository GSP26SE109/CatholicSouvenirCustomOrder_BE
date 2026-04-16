package org.example.catholicsouvenircustomorder.dto.response.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for checkout operation
 * Contains all orders created in a single checkout session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO {
    
    /**
     * Order group ID for payment (pay once for all orders)
     */
    private UUID orderGroupId;
    
    /**
     * List of orders created (one per artisan)
     */
    private List<OrderResponseDTO> orders;
    
    /**
     * Total amount for all orders combined
     */
    private BigDecimal totalAmount;
    
    /**
     * Number of orders created
     */
    private int orderCount;
    
    /**
     * Message to display to user
     */
    private String message;
}
