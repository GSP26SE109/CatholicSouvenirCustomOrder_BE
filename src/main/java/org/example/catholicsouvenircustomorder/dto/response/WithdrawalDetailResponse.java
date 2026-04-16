package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WithdrawalDetailResponse extends WithdrawalResponse {
    
    private String fullBankAccountNumber; // Only for owner and admin
    
    private UUID walletTransactionId;
    
    private LocalDateTime cancelledAt;
    
    private UUID processedById;
}
