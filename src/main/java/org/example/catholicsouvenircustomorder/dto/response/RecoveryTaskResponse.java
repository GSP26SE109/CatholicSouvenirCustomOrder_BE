package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for recovery task response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryTaskResponse {
    private UUID taskId;
    
    // Artisan info
    private UUID artisanId;
    private String artisanName;
    private String email;
    private String phone;
    
    // Customer info (for refund)
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    // Financial info
    private BigDecimal refundAmount;
    private BigDecimal artisanAvailableBalance;
    private BigDecimal artisanLockedBalance;
    
    // Order info
    private UUID orderId;
    private String reason;
    
    // Status
    private LocalDateTime createdAt;
    private String status;
    private Boolean actionCompleted;
}
