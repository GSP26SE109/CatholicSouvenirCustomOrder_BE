package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.catholicsouvenircustomorder.model.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {
    
    private UUID withdrawalId;
    
    private BigDecimal amount;
    
    private WithdrawalStatus status;
    
    private String bankName;
    
    private String bankAccountNumber; // Masked (e.g., "****1234")
    
    private String bankAccountName;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;
    
    private String processedByName;
    
    private String rejectionReason;
    
    private String reason; // Lý do rút tiền từ thợ thủ công
    
    // Artisan info
    private UUID artisanId;
    
    private String artisanName;
    
    private String artisanEmail;
}
