package org.example.catholicsouvenircustomorder.dto.request;

import lombok.Data;

/**
 * Request DTO for approving a withdrawal request
 * Note: withdrawalId is taken from path parameter, not from request body
 */
@Data
public class ApproveWithdrawalRequest {
    
    private String note; // Optional note from admin
}
