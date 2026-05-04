package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for order cancellation
 * Requirements: 10.1, 10.2, 3.1
 * Subtask 9.3 - Validation for cancellation requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {
    
    /**
     * Cancellation reason
     * - For Artisan: minimum 20 characters (enforced in service layer)
     * - For Customer: optional
     * Requirements: 3.1
     */
    private String reason;
}
