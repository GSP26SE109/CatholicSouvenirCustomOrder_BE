package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for commission rate information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRateResponse {
    
    /**
     * Current commission rate percentage (0-100)
     */
    private BigDecimal commissionRate;
    
    /**
     * Description of the commission rate
     */
    private String description;
}
