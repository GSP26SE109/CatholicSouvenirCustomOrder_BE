package org.example.catholicsouvenircustomorder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.CustomOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for custom order information.
 * Contains order summary with request info, artisan, status, and total price.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomOrderResponse {
    
    // Order identification
    private UUID customOrderId;
    private CustomOrderStatus status;
    
    // Request reference
    private UUID requestId;
    
    // Customer information
    private UUID customerId;
    private String customerName;
    
    // Artisan information
    private UUID artisanId;
    private String artisanName;
    
    // Template information
    private UUID templateId;
    private String templateName;
    
    // Pricing
    private BigDecimal totalPrice;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Check if order can be cancelled
     */
    public boolean canBeCancelled() {
        return status != CustomOrderStatus.COMPLETED 
            && status != CustomOrderStatus.CANCELLED;
    }
    
    /**
     * Check if order is in production
     */
    public boolean isInProduction() {
        return status == CustomOrderStatus.IN_PRODUCTION;
    }
    
    /**
     * Check if order is completed
     */
    public boolean isCompleted() {
        return status == CustomOrderStatus.COMPLETED;
    }
}
