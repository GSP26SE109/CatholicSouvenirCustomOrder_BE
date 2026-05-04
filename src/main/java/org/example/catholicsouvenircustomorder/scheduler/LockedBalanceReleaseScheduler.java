package org.example.catholicsouvenircustomorder.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.model.CustomOrderStage;
import org.example.catholicsouvenircustomorder.model.StageStatus;
import org.example.catholicsouvenircustomorder.model.Wallet;
import org.example.catholicsouvenircustomorder.repository.CustomOrderStageRepository;
import org.example.catholicsouvenircustomorder.repository.WalletRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to release locked balance (30%) after stage completion + 3 days
 * Runs daily at 2 AM to check and release eligible locked balances
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockedBalanceReleaseScheduler {
    
    private final CustomOrderStageRepository stageRepository;
    private final WalletRepository walletRepository;
    
    private static final int LOCK_PERIOD_DAYS = 3;
    private static final BigDecimal LOCKED_PERCENTAGE = new BigDecimal("0.30");
    
    /**
     * Release locked balance for completed stages older than 3 days
     * Runs every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void releaseLockedBalances() {
        log.info("Starting locked balance release job");
        
        try {
            // Calculate the cutoff date (3 days ago)
            LocalDateTime releaseDate = LocalDateTime.now().minusDays(LOCK_PERIOD_DAYS);
            
            // Find completed stages that are older than 3 days and haven't released balance yet
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
            
            log.info("Locked balance release job completed: {} successful, {} failed", 
                successCount, failCount);
            
        } catch (Exception e) {
            log.error("Error in locked balance release job", e);
        }
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
}
