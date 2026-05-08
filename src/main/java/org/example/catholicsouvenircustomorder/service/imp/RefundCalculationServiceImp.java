package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.response.CancellationEstimate;
import org.example.catholicsouvenircustomorder.dto.response.StageRefundCalculation;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.CustomOrderRepository;
import org.example.catholicsouvenircustomorder.service.RefundCalculationService;
import org.example.catholicsouvenircustomorder.service.SystemConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of RefundCalculationService
 * Calculates refunds with platform commission deduction
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RefundCalculationServiceImp implements RefundCalculationService {
    
    private final SystemConfigService systemConfigService;
    private final CustomOrderRepository customOrderRepository;
    
    @Override
    public List<StageRefundCalculation> calculateStageRefunds(CustomOrder order, CancellationInitiator initiator) {
        List<StageRefundCalculation> calculations = new ArrayList<>();
        
        // Get commission rate dynamically from SystemConfig
        BigDecimal commissionRate = systemConfigService.getCommissionRate();
        log.info("Calculating refunds for order {} with commission rate {}%", 
            order.getCustomOrderId(), commissionRate);
        
        for (CustomOrderStage stage : order.getStages()) {
            // Skip unpaid stages
            if (!stage.getIsPaid()) {
                log.debug("Skipping unpaid stage: {}", stage.getName());
                continue;
            }
            
            // Calculate refund percentage based on stage status and initiator
            BigDecimal refundPercentage = getRefundPercentage(stage, initiator);
            
            // Calculate gross refund (before commission)
            BigDecimal grossRefund = stage.getAmount()
                .multiply(refundPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            
            // Calculate platform commission (always deducted)
            BigDecimal platformCommission = BigDecimal.ZERO;
            BigDecimal netRefund = grossRefund;
            
            if (grossRefund.compareTo(BigDecimal.ZERO) > 0) {
                platformCommission = grossRefund
                    .multiply(commissionRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                netRefund = grossRefund.subtract(platformCommission);
            }
            
            String refundReason = getRefundReason(stage, initiator);
            
            StageRefundCalculation calculation = new StageRefundCalculation(
                stage.getStageId(),
                stage.getName(),
                stage.getAmount(),
                refundPercentage,
                grossRefund,
                platformCommission,
                netRefund,
                refundReason
            );
            
            calculations.add(calculation);
            
            log.debug("Stage {} refund: gross={}, commission={}, net={}", 
                stage.getName(), grossRefund, platformCommission, netRefund);
        }
        
        return calculations;
    }
    
    @Override
    public CancellationEstimate calculateRefundEstimate(UUID orderId, CancellationInitiator initiator) {
        // Get order
        CustomOrder order = customOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        
        // Calculate stage refunds
        List<StageRefundCalculation> stageBreakdown = calculateStageRefunds(order, initiator);
        
        // Calculate totals
        BigDecimal grossRefundAmount = stageBreakdown.stream()
            .map(StageRefundCalculation::getGrossRefund)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal platformCommission = stageBreakdown.stream()
            .map(StageRefundCalculation::getPlatformCommission)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netRefundAmount = stageBreakdown.stream()
            .map(StageRefundCalculation::getNetRefund)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Check if order can be cancelled
        boolean canCancel = !stageBreakdown.isEmpty() && 
            !order.getStatus().name().startsWith("CANCELLED");
        
        // Check if artisan has sufficient balance (check total balance, not just available)
        // Because locked balance can be unlocked for refund if needed
        Wallet artisanWallet = order.getArtisan().getWallet();
        BigDecimal totalBalance = artisanWallet.getBalance();
        BigDecimal availableBalance = artisanWallet.getAvailableBalance();
        boolean artisanHasSufficientBalance = totalBalance.compareTo(netRefundAmount) >= 0;
        
        // Log for debugging
        if (!artisanHasSufficientBalance && availableBalance.compareTo(netRefundAmount) < 0) {
            log.info("Artisan has insufficient available balance ({}) but may have locked balance. Total balance: {}, Required: {}", 
                availableBalance, totalBalance, netRefundAmount);
        }
        
        log.info("Refund estimate for order {}: gross={}, commission={}, net={}, canCancel={}, sufficientBalance={}",
            orderId, grossRefundAmount, platformCommission, netRefundAmount, canCancel, artisanHasSufficientBalance);
        
        return new CancellationEstimate(
            grossRefundAmount,
            platformCommission,
            netRefundAmount,
            stageBreakdown,
            canCancel,
            artisanHasSufficientBalance
        );
    }
    
    /**
     * Get refund percentage based on stage status and initiator
     * Requirements: 2.1, 2.2, 2.3, 3.2, 7.1, 7.2, 7.3, 7.4
     */
    private BigDecimal getRefundPercentage(CustomOrderStage stage, CancellationInitiator initiator) {
        StageStatus status = stage.getStatus();
        
        // COMPLETED stages - no refund
        if (status == StageStatus.COMPLETED) {
            return BigDecimal.ZERO;
        }
        
        // PAID (NOT_STARTED) stages - full refund
        if (status == StageStatus.PAID) {
            return new BigDecimal("100");
        }
        
        // IN_PROGRESS stages - depends on who cancels
        if (status == StageStatus.IN_PROGRESS) {
            if (initiator == CancellationInitiator.ARTISAN) {
                // Artisan cancels - full refund (100%)
                return new BigDecimal("100");
            } else {
                // Customer cancels - partial refund (50%)
                return new BigDecimal("50");
            }
        }
        
        // Default: no refund
        return BigDecimal.ZERO;
    }
    
    /**
     * Get human-readable refund reason
     */
    private String getRefundReason(CustomOrderStage stage, CancellationInitiator initiator) {
        StageStatus status = stage.getStatus();
        
        if (status == StageStatus.COMPLETED) {
            return "Stage completed - no refund";
        }
        
        if (status == StageStatus.PAID) {
            return "Stage not started - full refund";
        }
        
        if (status == StageStatus.IN_PROGRESS) {
            if (initiator == CancellationInitiator.ARTISAN) {
                return "Artisan cancellation - full refund";
            } else {
                return "Customer cancellation - partial refund (50%)";
            }
        }
        
        return "No refund applicable";
    }
}
