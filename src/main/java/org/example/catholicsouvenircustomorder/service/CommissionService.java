package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.CommissionCalculation;
import org.example.catholicsouvenircustomorder.model.WalletTransaction;

import java.math.BigDecimal;

/**
 * Service for handling commission calculations and application
 * Manages platform commission fees for artisan transactions
 */
public interface CommissionService {
    
    /**
     * Calculate commission amount and net amount from original amount and rate
     * 
     * @param amount Original payment amount
     * @param rate Commission rate (0-100%)
     * @return CommissionCalculation containing original, commission, and net amounts
     * @throws BadRequestException if net amount would be <= 0
     */
    CommissionCalculation calculateCommission(BigDecimal amount, BigDecimal rate);
    
    /**
     * Apply commission fee and rate to a wallet transaction
     * Sets the commissionFee and commissionRate fields on the transaction
     * 
     * @param transaction WalletTransaction to apply commission to
     * @param commissionAmount Commission amount to set
     * @param commissionRate Commission rate to set
     */
    void applyCommissionToWalletTransaction(WalletTransaction transaction, 
                                           BigDecimal commissionAmount, 
                                           BigDecimal commissionRate);
}
