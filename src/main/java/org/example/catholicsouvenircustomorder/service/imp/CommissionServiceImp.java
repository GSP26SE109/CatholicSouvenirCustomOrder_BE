package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.CommissionCalculation;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.model.WalletTransaction;
import org.example.catholicsouvenircustomorder.service.CommissionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementation of CommissionService
 * Handles commission calculation and application to wallet transactions
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionServiceImp implements CommissionService {
    
    @Override
    public CommissionCalculation calculateCommission(BigDecimal amount, BigDecimal rate) {
        // If rate is null or zero, no commission
        if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Commission rate is 0 or null, no commission applied");
            return new CommissionCalculation(amount, BigDecimal.ZERO, amount);
        }
        
        // Calculate commission: amount * rate / 100
        // Round to 2 decimal places using HALF_UP rounding mode
        BigDecimal commissionAmount = amount
            .multiply(rate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        
        // Calculate net amount: amount - commission
        BigDecimal netAmount = amount.subtract(commissionAmount);
        
        // Validate net amount must be greater than 0
        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Commission calculation resulted in net amount <= 0. Amount: {}, Rate: {}, Commission: {}, Net: {}", 
                amount, rate, commissionAmount, netAmount);
            throw new BadRequestException("Số tiền sau khi trừ phí phải lớn hơn 0");
        }
        
        log.info("Commission calculated: amount={}, rate={}%, commission={}, net={}", 
            amount, rate, commissionAmount, netAmount);
        
        return new CommissionCalculation(amount, commissionAmount, netAmount);
    }
    
    @Override
    public void applyCommissionToWalletTransaction(WalletTransaction transaction, 
                                                   BigDecimal commissionAmount, 
                                                   BigDecimal commissionRate) {
        transaction.setCommissionFee(commissionAmount);
        transaction.setCommissionRate(commissionRate);
        
        log.info("Commission applied to wallet transaction {}: fee={}, rate={}%", 
            transaction.getTransactionId(), commissionAmount, commissionRate);
    }
}
