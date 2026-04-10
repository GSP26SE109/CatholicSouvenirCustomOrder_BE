package org.example.catholicsouvenircustomorder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.CustomOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed response DTO for custom order information.
 * Contains full order information including customer details, artisan details,
 * template information, customization inputs, and AI concept image.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomOrderDetailResponse {
    
    // Order identification
    private UUID customOrderId;
    private CustomOrderStatus status;
    
    // Request reference
    private UUID requestId;
    
    // Customer information
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    // Artisan information
    private UUID artisanId;
    private String artisanName;
    private String artisanEmail;
    private String artisanPhone;
    
    // Request description and AI image
    private String description;
    private String aiConceptImageUrl;
    
    // Pricing
    private BigDecimal totalPrice;
    
    // Stages (for Request-Based orders)
    private List<CustomOrderStageResponse> stages;
    
    // Payment status
    private boolean fullyPaid;
    
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
     * Check if order can be updated
     */
    public boolean canBeUpdated() {
        return status != CustomOrderStatus.COMPLETED 
            && status != CustomOrderStatus.CANCELLED;
    }
    
    /**
     * Check if order is active
     */
    public boolean isActive() {
        return status == CustomOrderStatus.CONFIRMED 
            || status == CustomOrderStatus.IN_PRODUCTION;
    }
}
