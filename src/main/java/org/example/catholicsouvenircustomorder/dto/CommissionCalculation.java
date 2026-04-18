package org.example.catholicsouvenircustomorder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for commission calculation results
 * Contains original amount, commission amount, and net amount after commission
 */
@Data
@AllArgsConstructor
public class CommissionCalculation {
    
    /**
     * Original payment amount before commission deduction
     */
    private BigDecimal originalAmount;
    
    /**
     * Commission amount deducted (originalAmount × rate / 100)
     */
    private BigDecimal commissionAmount;
    
    /**
     * Net amount after commission deduction (originalAmount - commissionAmount)
     */
    private BigDecimal netAmount;
}
