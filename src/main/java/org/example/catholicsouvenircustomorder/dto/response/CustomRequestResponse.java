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
import java.util.UUID;

/**
 * Response DTO for custom request information (Request-Based flow only).
 * CustomRequest is only used for request-based custom orders.
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
    
    // Selected artisan information (after customer selects)
    private UUID artisanId;
    private String artisanName;
    
    // Request description
    private String description;
    
    // AI image information
    private String aiConceptImageUrl;
    private String aiImagePrompt;
    private Integer imageGenCount;
    private Integer maxImageGenCount;
    
    // Budget range
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
        return status == CustomRequestStatus.DRAFT;
    }
}
