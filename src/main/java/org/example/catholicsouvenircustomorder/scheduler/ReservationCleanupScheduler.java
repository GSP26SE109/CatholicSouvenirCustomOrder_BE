package org.example.catholicsouvenircustomorder.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.model.OrderGroup;
import org.example.catholicsouvenircustomorder.repository.OrderGroupRepository;
import org.example.catholicsouvenircustomorder.service.InventoryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler to automatically release inventory reservations for expired pending orders
 * Runs every 5 minutes to check for orders pending payment for more than 30 minutes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {
    
    private final OrderGroupRepository orderGroupRepository;
    private final InventoryService inventoryService;
    
    /**
     * Release reservations for orders that have been pending for more than 30 minutes
     * Runs every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    @Transactional
    public void releaseExpiredReservations() {
        log.info("🔄 Starting expired reservation cleanup job");
        
        try {
            LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(30);
            
            // Find all PENDING order groups older than 30 minutes
            List<OrderGroup> expiredOrders = orderGroupRepository
                .findByStatusAndUpdatedAtBefore("PENDING", expirationTime);
            
            if (expiredOrders.isEmpty()) {
                log.info("✅ No expired reservations found");
                return;
            }
            
            log.info("📦 Found {} expired order groups to process", expiredOrders.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (OrderGroup orderGroup : expiredOrders) {
                try {
                    log.info("Releasing reservations for expired order group: {} (created: {})",
                        orderGroup.getGroupId(), orderGroup.getUpdatedAt());
                    
                    inventoryService.releaseReservations(orderGroup);
                    
                    // Mark order group as EXPIRED
                    orderGroup.setStatus("EXPIRED");
                    orderGroup.setUpdatedAt(LocalDateTime.now());
                    orderGroupRepository.save(orderGroup);
                    
                    successCount++;
                    log.info("✅ Released reservations for order group: {}", orderGroup.getGroupId());
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ Failed to release reservations for order group {}: {}",
                        orderGroup.getGroupId(), e.getMessage(), e);
                }
            }
            
            log.info("✅ Reservation cleanup completed: {} succeeded, {} failed", 
                successCount, failCount);
            
        } catch (Exception e) {
            log.error("❌ Error in reservation cleanup job: {}", e.getMessage(), e);
        }
    }
}
