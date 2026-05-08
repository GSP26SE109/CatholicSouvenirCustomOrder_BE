package org.example.catholicsouvenircustomorder.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.RecoveryTaskResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.repository.NotificationRepository;
import org.example.catholicsouvenircustomorder.repository.WalletRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin controller for offline recovery tasks
 * Requirements: 5.3, 9.3, 9.4, 9.5
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRecoveryController {
    
    private final NotificationRepository notificationRepository;
    private final ArtisanRepository artisanRepository;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    
    /**
     * Get all offline recovery tasks
     * GET /api/admin/recovery-tasks
     * Requirements: 5.3, 9.3
     */
    @GetMapping("/recovery-tasks")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<List<RecoveryTaskResponse>>> getRecoveryTasks() {
        log.info("Admin fetching recovery tasks");
        
        // Find all notifications with REVIEW_RECOVERY action type
        List<Notification> notifications = notificationRepository
            .findByActionTypeOrderByCreatedAtDesc(NotificationAction.REVIEW_RECOVERY);
        
        List<RecoveryTaskResponse> tasks = new ArrayList<>();
        
        for (Notification notification : notifications) {
            try {
                // Parse metadata
                String metadata = notification.getMetadata();
                if (metadata == null || metadata.isEmpty()) {
                    log.warn("Notification {} has no metadata", notification.getNotificationId());
                    continue;
                }
                
                // Parse metadata string (format: key=value;key=value;...)
                String[] pairs = metadata.split(";");
                UUID artisanId = null;
                String artisanName = null;
                String artisanEmail = null;
                String artisanPhone = null;
                UUID customerId = null;
                String customerName = null;
                String customerEmail = null;
                String customerPhone = null;
                BigDecimal refundAmount = null;
                BigDecimal availableBalance = null;
                BigDecimal lockedBalance = null;
                UUID orderId = null;
                String reason = null;
                
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        switch (key) {
                            case "artisanId":
                                artisanId = UUID.fromString(value);
                                break;
                            case "artisanName":
                                artisanName = value;
                                break;
                            case "artisanEmail":
                                artisanEmail = value;
                                break;
                            case "artisanPhone":
                                artisanPhone = value;
                                break;
                            case "customerId":
                                customerId = UUID.fromString(value);
                                break;
                            case "customerName":
                                customerName = value;
                                break;
                            case "customerEmail":
                                customerEmail = value;
                                break;
                            case "customerPhone":
                                customerPhone = value;
                                break;
                            case "refundAmount":
                                refundAmount = new BigDecimal(value);
                                break;
                            case "availableBalance":
                                availableBalance = new BigDecimal(value);
                                break;
                            case "lockedBalance":
                                lockedBalance = new BigDecimal(value);
                                break;
                            case "orderId":
                                orderId = UUID.fromString(value);
                                break;
                            case "reason":
                                reason = value;
                                break;
                        }
                    }
                }
                
                // If artisanId not in metadata, log warning
                if (artisanId == null) {
                    log.warn("Notification {} missing artisanId in metadata", notification.getNotificationId());
                    continue; // Skip this notification
                }
                
                RecoveryTaskResponse task = RecoveryTaskResponse.builder()
                    .taskId(notification.getNotificationId())
                    .artisanId(artisanId)
                    .artisanName(artisanName)
                    .email(artisanEmail)
                    .phone(artisanPhone)
                    .customerId(customerId)
                    .customerName(customerName)
                    .customerEmail(customerEmail)
                    .customerPhone(customerPhone)
                    .refundAmount(refundAmount)
                    .artisanAvailableBalance(availableBalance)
                    .artisanLockedBalance(lockedBalance)
                    .orderId(orderId)
                    .reason(reason)
                    .createdAt(notification.getCreatedAt())
                    .status(notification.getActionCompleted() ? "RECOVERED" : "PENDING")
                    .actionCompleted(notification.getActionCompleted())
                    .build();
                
                tasks.add(task);
                
            } catch (Exception e) {
                log.error("Error parsing recovery task notification {}: {}", 
                    notification.getNotificationId(), e.getMessage());
            }
        }
        
        log.info("Found {} recovery tasks", tasks.size());
        return ResponseEntity.ok(BaseResponse.<List<RecoveryTaskResponse>>builder()
                .code(200)
                .message("Lấy danh sách recovery tasks thành công")
                .data(tasks)
                .build());
    }
    
    /**
     * Mark recovery task as recovered
     * POST /api/admin/recovery-tasks/{taskId}/mark-recovered
     * Requirements: 9.5
     */
    @PostMapping("/recovery-tasks/{taskId}/mark-recovered")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse> markRecovered(@PathVariable UUID taskId) {
        log.info("Admin marking recovery task {} as recovered", taskId);
        
        Notification notification = notificationRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Recovery task not found"));
        
        if (notification.getActionType() != NotificationAction.REVIEW_RECOVERY) {
            return ResponseEntity.badRequest()
                .body(BaseResponse.error(400, "This is not a recovery task notification"));
        }
        
        notification.setActionCompleted(true);
        notification.setActionCompletedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        log.info("Recovery task {} marked as recovered", taskId);
        return ResponseEntity.ok(BaseResponse.success("Recovery task đã được đánh dấu là đã xử lý", null));
    }
    
    /**
     * Blacklist an artisan and seize locked balance
     * POST /api/admin/artisans/{artisanId}/blacklist
     * Requirements: 5.4, 9.4
     * 
     * This will:
     * 1. Blacklist the artisan
     * 2. Seize all locked balance from artisan wallet
     * 3. Transfer locked balance to platform admin wallet
     * 4. Create wallet transaction records
     */
    @PostMapping("/artisans/{artisanId}/blacklist")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public ResponseEntity<BaseResponse<BlacklistResponse>> blacklistArtisan(@PathVariable UUID artisanId) {
        log.info("Admin blacklisting artisan {}", artisanId);
        
        Artisan artisan = artisanRepository.findById(artisanId)
            .orElseThrow(() -> new ResourceNotFoundException("Artisan not found"));
        
        Wallet artisanWallet = artisan.getWallet();
        BigDecimal lockedBalance = artisanWallet.getLockedBalance() != null ? artisanWallet.getLockedBalance() : BigDecimal.ZERO;
        Account artisanAccount = artisan.getAccount();
        
        // 1. Blacklist artisan and revoke verification
        artisan.setIsBlacklisted(true);
        artisanAccount.setVerified(false); // Revoke account verification
        artisanRepository.save(artisan);
        accountRepository.save(artisanAccount);
        
        log.info("Blacklisted artisan {} and revoked account verification", artisanId);
        
        // 2. Seize locked balance if any
        BigDecimal seizedAmount = BigDecimal.ZERO;
        if (lockedBalance.compareTo(BigDecimal.ZERO) > 0) {
            // Deduct locked balance from artisan wallet
            artisanWallet.setBalance(artisanWallet.getBalance().subtract(lockedBalance));
            artisanWallet.setLockedBalance(BigDecimal.ZERO);
            walletRepository.save(artisanWallet);
            
            // Transfer to platform admin wallet
            Account platformAdmin = accountRepository.findByRole_Name("ADMIN").stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Platform admin not found"));
            
            Wallet adminWallet = walletRepository.findByAccount(platformAdmin)
                    .orElseGet(() -> {
                        Wallet newWallet = new Wallet();
                        newWallet.setAccount(platformAdmin);
                        newWallet.setBalance(BigDecimal.ZERO);
                        newWallet.setLockedBalance(BigDecimal.ZERO);
                        return walletRepository.save(newWallet);
                    });
            
            adminWallet.setBalance(adminWallet.getBalance().add(lockedBalance));
            walletRepository.save(adminWallet);
            
            seizedAmount = lockedBalance;
            
            log.info("Seized {} VND locked balance from blacklisted artisan {} and transferred to admin wallet", 
                    lockedBalance, artisanId);
        }
        
        BlacklistResponse response = BlacklistResponse.builder()
                .artisanId(artisanId)
                .artisanName(artisan.getAccount().getFullName())
                .blacklisted(true)
                .accountVerified(false)
                .seizedLockedBalance(seizedAmount)
                .message(seizedAmount.compareTo(BigDecimal.ZERO) > 0 
                        ? String.format("Artisan đã bị blacklist, thu hồi verification và thu hồi %s VND locked balance", seizedAmount)
                        : "Artisan đã bị blacklist và thu hồi verification (không có locked balance)")
                .build();
        
        log.info("Artisan {} has been blacklisted, verification revoked, seized amount: {}", artisanId, seizedAmount);
        return ResponseEntity.ok(BaseResponse.<BlacklistResponse>builder()
                .code(200)
                .message("Blacklist artisan thành công")
                .data(response)
                .build());
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlacklistResponse {
        private UUID artisanId;
        private String artisanName;
        private Boolean blacklisted;
        private Boolean accountVerified;
        private BigDecimal seizedLockedBalance;
        private String message;
    }
    
    /**
     * Remove artisan from blacklist
     * DELETE /api/admin/artisans/{artisanId}/blacklist
     * Requirements: 5.4, 9.4
     */
    @DeleteMapping("/artisans/{artisanId}/blacklist")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse> removeBlacklist(@PathVariable UUID artisanId) {
        log.info("Admin removing blacklist from artisan {}", artisanId);
        
        Artisan artisan = artisanRepository.findById(artisanId)
            .orElseThrow(() -> new ResourceNotFoundException("Artisan not found"));
        
        artisan.setIsBlacklisted(false);
        artisanRepository.save(artisan);
        
        log.info("Artisan {} has been removed from blacklist", artisanId);
        return ResponseEntity.ok(BaseResponse.success("Artisan đã được gỡ khỏi blacklist", null));
    }
}
