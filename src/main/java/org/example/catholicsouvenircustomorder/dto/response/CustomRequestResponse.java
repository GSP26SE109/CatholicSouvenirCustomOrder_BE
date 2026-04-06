package org.example.catholicsouvenircustomorder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;
import org.example.catholicsouvenircustomorder.model.RequestType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for custom request information.
 * Contains complete request details including template, customer, artisan, and AI image data.
 * Supports both TEMPLATE_BASED and REQUEST_BASED flows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomRequestResponse {
    
    // Request identification
    private UUID requestId;
    private CustomRequestStatus status;
    private RequestType requestType;
    
    // Customer information
    private UUID customerId;
    private String customerName;
    
    // Template information (only for TEMPLATE_BASED)
    private UUID templateId;
    private String templateName;
    private String templateDescription;
    private BigDecimal basePrice;
    
    // Artisan information (only for TEMPLATE_BASED)
    private UUID artisanId;
    private String artisanName;
    
    // Customization details
    private Map<String, String> zoneInputs;
    private String additionalDescription;
    
    // AI image information
    private String aiConceptImageUrl;
    private String aiImagePrompt;
    private Integer imageGenCount;
    private Integer maxImageGenCount;
    
    // Pricing (totalPrice for TEMPLATE_BASED, budget range for REQUEST_BASED)
    private BigDecimal totalPrice;
    private BigDecimal minBudget;
    private BigDecimal maxBudget;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Check if AI image can be regenerated
     */
    public boolean canRegenerateImage() {
        return imageGenCount != null && maxImageGenCount != null 
            && imageGenCount < maxImageGenCount;
    }
    
    /**
     * Check if request can be modified by customer
     */
    public boolean canBeModified() {
        return status == CustomRequestStatus.PENDING;
    }
}
