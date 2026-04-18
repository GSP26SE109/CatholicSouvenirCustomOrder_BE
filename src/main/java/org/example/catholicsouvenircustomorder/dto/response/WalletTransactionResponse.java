package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.WalletTransactionType;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {
    private UUID transactionId;
    private UUID walletId;
    private WalletTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private UUID paymentId;
    private UUID stagePaymentId;
    private LocalDateTime createdAt;
    
    // Commission fields
    private BigDecimal commissionFee;
    private BigDecimal commissionRate;
    
    // Calculated field: original amount before commission deduction
    private BigDecimal originalAmount;
    
    // Formatted commission fee with VND currency
    private String commissionFeeFormatted;
    
    /**
     * Calculate original amount (amount + commissionFee)
     * This represents the amount before commission was deducted
     */
    public BigDecimal getOriginalAmount() {
        if (commissionFee != null && commissionFee.compareTo(BigDecimal.ZERO) > 0) {
            return amount.add(commissionFee);
        }
        return amount;
    }
    
    /**
     * Format commission fee with 2 decimal places and VND currency
     */
    public String getCommissionFeeFormatted() {
        if (commissionFee == null || commissionFee.compareTo(BigDecimal.ZERO) == 0) {
            return "0 VND";
        }
        
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(commissionFee) + " VND";
    }
}
