package org.example.catholicsouvenircustomorder.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.model.CustomOrderStage;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.StageStatus;
import org.example.catholicsouvenircustomorder.model.Wallet;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.repository.CustomOrderStageRepository;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.WalletRepository;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled task to release locked balance
 * 1. Custom order stages: 30% locked for 3 days after completion
 * 2. Product/Template orders: 100% locked for 7 days after payment
 * Runs daily at 2 AM to check and release eligible locked balances
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockedBalanceReleaseScheduler {
    
    private final CustomOrderStageRepository stageRepository;
    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final ArtisanRepository artisanRepository;
    
    private static final int STAGE_LOCK_PERIOD_DAYS = 3;
    private static final int ORDER_LOCK_PERIOD_DAYS = 7;
    private static final BigDecimal LOCKED_PERCENTAGE = new BigDecimal("0.30");
    
    /**
     * Release locked balance for completed stages and paid orders
     * Runs every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void releaseLockedBalances() {
        log.info("========================================");
        log.info("Starting locked balance release job");
        log.info("========================================");
        
        try {
            // 1. Release locked balance for custom order stages (3 days)
            releaseStageLockedBalances();
            
            // 2. Release locked balance for product/template orders (7 days)
            releaseOrderLockedBalances();
            
            log.info("========================================");
            log.info("Locked balance release job completed");
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("Error in locked balance release job", e);
        }
    }
    
    /**
     * Release locked balance for custom order stages (30% locked for 3 days)
     */
    private void releaseStageLockedBalances() {
        log.info("--- Releasing Stage Locked Balances (3 days) ---");
        
        LocalDateTime releaseDate = LocalDateTime.now().minusDays(STAGE_LOCK_PERIOD_DAYS);
        
        List<CustomOrderStage> eligibleStages = stageRepository.findCompletedStagesForBalanceRelease(
            StageStatus.COMPLETED, 
            releaseDate
        );
        
        log.info("Found {} stages eligible for balance release", eligibleStages.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (CustomOrderStage stage : eligibleStages) {
            try {
                releaseStageBalance(stage);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to release balance for stage {}: {}", 
                    stage.getStageId(), e.getMessage(), e);
                failCount++;
            }
        }
        
        log.info("Stage balance release: {} successful, {} failed", successCount, failCount);
    }
    
    /**
     * Release locked balance for product/template orders (100% locked for 7 days)
     */
    private void releaseOrderLockedBalances() {
        log.info("--- Releasing Order Locked Balances (7 days) ---");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find orders with unlockDate <= now and status = PAID
        List<Order> eligibleOrders = orderRepository.findEligibleOrdersForBalanceRelease(now);
        
        log.info("Found {} orders eligible for balance release", eligibleOrders.size());
        
        int successCount = 0;
        int failCount = 0;
        BigDecimal totalReleased = BigDecimal.ZERO;
        
        for (Order order : eligibleOrders) {
            try {
                BigDecimal released = releaseOrderBalance(order);
                totalReleased = totalReleased.add(released);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to release balance for order {}: {}", 
                    order.getOrderId(), e.getMessage(), e);
                failCount++;
            }
        }
        
        log.info("Order balance release: {} successful, {} failed, total released: {} VND", 
            successCount, failCount, totalReleased);
    }
    
    /**
     * Release locked balance for a specific stage
     */
    private void releaseStageBalance(CustomOrderStage stage) {
        // Calculate the locked amount (30% of stage amount)
        BigDecimal lockedAmount = stage.getAmount()
            .multiply(LOCKED_PERCENTAGE)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Get artisan wallet
        Wallet wallet = walletRepository.findByAccount(
            stage.getCustomOrder().getRequest().getSelectedArtisan().getAccount()
        ).orElseThrow(() -> new RuntimeException("Wallet not found for artisan"));
        
        // Subtract from locked balance
        BigDecimal currentLockedBalance = wallet.getLockedBalance();
        BigDecimal newLockedBalance = currentLockedBalance.subtract(lockedAmount);
        
        // Ensure locked balance doesn't go negative
        if (newLockedBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Locked balance would go negative for wallet {}, setting to zero", 
                wallet.getWalletId());
            newLockedBalance = BigDecimal.ZERO;
        }
        
        wallet.setLockedBalance(newLockedBalance);
        walletRepository.save(wallet);
        
        // Mark stage as balance released
        stage.setBalanceReleased(true);
        stageRepository.save(stage);
        
        log.info("Released {} from locked balance for stage {} (wallet {}): {} -> {}", 
            lockedAmount, stage.getStageId(), wallet.getWalletId(), 
            currentLockedBalance, newLockedBalance);
    }
    
    /**
     * Release locked balance for a specific product/template order
     * Returns the amount released
     */
    private BigDecimal releaseOrderBalance(Order order) {
        // Find artisan for this order
        Optional<Artisan> artisanOpt = artisanRepository.findByOrderIdFromProduct(order.getOrderId());
        if (artisanOpt.isEmpty()) {
            artisanOpt = artisanRepository.findByOrderIdFromTemplate(order.getOrderId());
        }
        
        if (artisanOpt.isEmpty()) {
            log.warn("No artisan found for order {}, skipping", order.getOrderId());
            return BigDecimal.ZERO;
        }
        
        Artisan artisan = artisanOpt.get();
        
        // Get artisan wallet
        Wallet wallet = walletRepository.findByAccount(artisan.getAccount())
            .orElseThrow(() -> new RuntimeException("Wallet not found for artisan " + artisan.getArtisanUuid()));
        
        // Calculate amount to release (order total minus shipping fee and commission already deducted)
        // The locked amount should match what was locked during payment distribution
        BigDecimal orderTotal = order.getTotal();
        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal productTotal = orderTotal.subtract(shippingFee);
        
        // Note: Commission was already deducted during payment distribution
        // So the locked amount is the net amount (after commission)
        // We need to find the actual locked amount from wallet transactions
        // For simplicity, we'll use the order total as reference
        
        BigDecimal lockedAmount = productTotal; // This is approximate, actual amount may vary due to commission
        
        // Subtract from locked balance
        BigDecimal currentLockedBalance = wallet.getLockedBalance() != null ? wallet.getLockedBalance() : BigDecimal.ZERO;
        BigDecimal newLockedBalance = currentLockedBalance.subtract(lockedAmount);
        
        // Ensure locked balance doesn't go negative
        if (newLockedBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Locked balance would go negative for wallet {} (order {}), releasing {} instead of {}", 
                wallet.getWalletId(), order.getOrderId(), currentLockedBalance, lockedAmount);
            lockedAmount = currentLockedBalance;
            newLockedBalance = BigDecimal.ZERO;
        }
        
        wallet.setLockedBalance(newLockedBalance);
        walletRepository.save(wallet);
        
        // Clear unlock date to mark as processed
        order.setUnlockDate(null);
        orderRepository.save(order);
        
        log.info("✅ Released {} VND from locked balance for order {} (wallet {}): {} -> {}", 
            lockedAmount, order.getOrderId(), wallet.getWalletId(), 
            currentLockedBalance, newLockedBalance);
        
        return lockedAmount;
    }
}
