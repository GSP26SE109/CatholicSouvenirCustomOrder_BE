package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for commission configuration details (Admin only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionConfigResponse {
    
    /**
     * Current commission rate percentage (0-100)
     */
    private BigDecimal commissionRate;
    
    /**
     * Timestamp when the rate was last updated
     */
    private LocalDateTime updatedAt;
    
    /**
     * Email of the admin who last updated the rate
     */
    private String updatedBy;
    
    /**
     * Allowed time window for updates (e.g., "00:00 - 00:59")
     */
    private String allowedUpdateTime;
}
