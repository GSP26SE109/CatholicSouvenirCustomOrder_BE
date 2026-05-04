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
    private UUID artisanId;
    private String artisanName;
    private String email;
    private String phone;
    private BigDecimal refundAmount;
    private UUID orderId;
    private String reason;
    private LocalDateTime createdAt;
    private String status;
    private Boolean actionCompleted;
}
