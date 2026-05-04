package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.RecoveryTaskResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.model.Notification;
import org.example.catholicsouvenircustomorder.model.NotificationAction;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    
    /**
     * Get all offline recovery tasks
     * GET /api/admin/recovery-tasks
     * Requirements: 5.3, 9.3
     */
    @GetMapping("/recovery-tasks")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse> getRecoveryTasks() {
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
                BigDecimal refundAmount = null;
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
                            case "refundAmount":
                                refundAmount = new BigDecimal(value);
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
                
                // If artisanId not in metadata, try to get from relatedEntityId
                if (artisanId == null) {
                    artisanId = notification.getRelatedEntityId();
                }
                
                RecoveryTaskResponse task = RecoveryTaskResponse.builder()
                    .taskId(notification.getNotificationId())
                    .artisanId(artisanId)
                    .artisanName(artisanName)
                    .email(artisanEmail)
                    .phone(artisanPhone)
                    .refundAmount(refundAmount)
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
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách recovery tasks thành công", tasks));
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
     * Blacklist an artisan
     * POST /api/admin/artisans/{artisanId}/blacklist
     * Requirements: 5.4, 9.4
     */
    @PostMapping("/artisans/{artisanId}/blacklist")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse> blacklistArtisan(@PathVariable UUID artisanId) {
        log.info("Admin blacklisting artisan {}", artisanId);
        
        Artisan artisan = artisanRepository.findById(artisanId)
            .orElseThrow(() -> new ResourceNotFoundException("Artisan not found"));
        
        artisan.setBlacklisted(true);
        artisanRepository.save(artisan);
        
        log.info("Artisan {} has been blacklisted", artisanId);
        return ResponseEntity.ok(BaseResponse.success("Artisan đã bị blacklist", null));
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
        
        artisan.setBlacklisted(false);
        artisanRepository.save(artisan);
        
        log.info("Artisan {} has been removed from blacklist", artisanId);
        return ResponseEntity.ok(BaseResponse.success("Artisan đã được gỡ khỏi blacklist", null));
    }
}
