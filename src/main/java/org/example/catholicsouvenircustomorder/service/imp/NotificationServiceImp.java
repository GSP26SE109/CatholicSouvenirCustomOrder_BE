package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.response.NotificationResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.NotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.CustomRequestRepository;
import org.example.catholicsouvenircustomorder.repository.NotificationRepository;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImp implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final CustomRequestRepository customRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ========== Create Notification methods ==========
    
    @Override
    @Transactional
    public void notifyArtisanOfNewCustomRequest(UUID artisanId, UUID requestId, 
                                               String customerName, String description) {
        try {
            Account artisan = accountRepository.findById(artisanId)
                    .orElseThrow(() -> new NotFoundException("Artisan not found"));
            
            // Simple metadata as String (no JSON needed)
            String metadata = String.format("customerName=%s;description=%s", 
                customerName, description);
            
            Notification notification = new Notification();
            notification.setRecipient(artisan);
            notification.setType(NotificationType.NEW_CUSTOM_REQUEST);
            notification.setTitle("New Custom Order Request");
            notification.setMessage("You have a new custom order request from " + customerName);
            notification.setRelatedEntityId(requestId);
            notification.setRelatedEntityType(RelatedEntityType.CUSTOM_REQUEST);
            
            // Action required
            notification.setActionType(NotificationAction.ACCEPT_REQUEST);
            notification.setActionRequired(true);
            notification.setActionCompleted(false);
            notification.setPriority(NotificationPriority.HIGH);
            notification.setMetadata(metadata);
            
            notification = notificationRepository.save(notification);
            
            // Send real-time
            sendRealTimeNotification(artisanId, "/notifications", mapToResponse(notification));
            
            log.info("Sent custom request notification to artisan {}", artisanId);
            
        } catch (Exception e) {
            log.error("Error sending custom request notification", e);
        }
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfNewQuotation(UUID customerId, UUID quotationId, 
                                            String artisanName, Long price) {
        try {
            Account customer = accountRepository.findById(customerId)
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
            
            String metadata = String.format("artisanName=%s;price=%d", artisanName, price);
            
            Notification notification = new Notification();
            notification.setRecipient(customer);
            notification.setType(NotificationType.NEW_QUOTATION);
            notification.setTitle("New Quotation Received");
            notification.setMessage(String.format(
                "You received a new quotation from %s for %,d VNĐ", 
                artisanName, price
            ));
            notification.setRelatedEntityId(quotationId);
            notification.setRelatedEntityType(RelatedEntityType.QUOTATION);
            notification.setActionType(NotificationAction.VIEW_QUOTATION);
            notification.setActionRequired(false);
            notification.setPriority(NotificationPriority.NORMAL);
            notification.setMetadata(metadata);
            
            notification = notificationRepository.save(notification);
            
            sendRealTimeNotification(customerId, "/quotations", mapToResponse(notification));
            
            log.info("Sent quotation notification to customer {}", customerId);
            
        } catch (Exception e) {
            log.error("Error sending quotation notification", e);
        }
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfOrderCreated(UUID customerId, UUID orderId, 
                                            Long totalAmount, Integer stagesCount) {
        try {
            Account customer = accountRepository.findById(customerId)
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
            
            String metadata = String.format("totalAmount=%d;stagesCount=%d", 
                totalAmount, stagesCount);
            
            Notification notification = new Notification();
            notification.setRecipient(customer);
            notification.setType(NotificationType.ORDER_CREATED);
            notification.setTitle("Order Created");
            notification.setMessage(String.format(
                "Your custom order has been created with %d stages. Total: %,d VNĐ. Please pay Stage 1 to start.", 
                stagesCount, totalAmount
            ));
            notification.setRelatedEntityId(orderId);
            notification.setRelatedEntityType(RelatedEntityType.CUSTOM_ORDER);
            notification.setActionType(NotificationAction.PAY_STAGE);
            notification.setActionRequired(true);
            notification.setPriority(NotificationPriority.HIGH);
            notification.setMetadata(metadata);
            
            notification = notificationRepository.save(notification);
            
            sendRealTimeNotification(customerId, "/orders", mapToResponse(notification));
            
            log.info("Sent order creation notification to customer {}", customerId);
            
        } catch (Exception e) {
            log.error("Error sending order creation notification", e);
        }
    }
    
    @Override
    @Transactional
    public void notifyArtisanOfPayment(UUID artisanId, UUID stageId, String stageName, Long amount) {
        try {
            Account artisan = accountRepository.findById(artisanId)
                    .orElseThrow(() -> new NotFoundException("Artisan not found"));
            
            String metadata = String.format("stageName=%s;amount=%d", stageName, amount);
            
            Notification notification = new Notification();
            notification.setRecipient(artisan);
            notification.setType(NotificationType.PAYMENT_RECEIVED);
            notification.setTitle("Payment Received");
            notification.setMessage(String.format(
                "Payment received for %s: %,d VNĐ. You can start working.", 
                stageName, amount
            ));
            notification.setRelatedEntityId(stageId);
            notification.setRelatedEntityType(RelatedEntityType.STAGE);
            notification.setActionType(NotificationAction.COMPLETE_STAGE);
            notification.setActionRequired(false);
            notification.setPriority(NotificationPriority.NORMAL);
            notification.setMetadata(metadata);
            
            notification = notificationRepository.save(notification);
            
            sendRealTimeNotification(artisanId, "/payments", mapToResponse(notification));
            
            log.info("Sent payment notification to artisan {}", artisanId);
            
        } catch (Exception e) {
            log.error("Error sending payment notification", e);
        }
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfStageCompletion(UUID customerId, UUID stageId, String stageName,
                                               UUID nextStageId, String nextStageName, Long nextAmount) {
        try {
            Account customer = accountRepository.findById(customerId)
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
            
            String message;
            String metadata = String.format("stageId=%s;stageName=%s", stageId, stageName);
            
            if (nextStageId != null) {
                message = String.format(
                    "%s completed! Please pay %s (%,d VNĐ) to continue.", 
                    stageName, nextStageName, nextAmount
                );
                metadata += String.format(";nextStageId=%s;nextStageName=%s;nextAmount=%d",
                    nextStageId, nextStageName, nextAmount);
            } else {
                message = String.format(
                    "%s completed! Your order is finished and will be shipped soon.", 
                    stageName
                );
            }
            
            Notification notification = new Notification();
            notification.setRecipient(customer);
            notification.setType(NotificationType.STAGE_COMPLETED);
            notification.setTitle("Stage Completed");
            notification.setMessage(message);
            notification.setRelatedEntityId(stageId);
            notification.setRelatedEntityType(RelatedEntityType.STAGE);
            notification.setActionType(nextStageId != null ? NotificationAction.PAY_STAGE : NotificationAction.VIEW_ORDER);
            notification.setActionRequired(nextStageId != null);
            notification.setPriority(NotificationPriority.HIGH);
            notification.setMetadata(metadata);
            
            notification = notificationRepository.save(notification);
            
            sendRealTimeNotification(customerId, "/stage-updates", mapToResponse(notification));
            
            log.info("Sent stage completion notification to customer {}", customerId);
            
        } catch (Exception e) {
            log.error("Error sending stage completion notification", e);
        }
    }
    
    // ========== Query methods ==========
    
    @Override
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
                .findByRecipient_AccountIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::mapToResponse);
    }
    
    @Override
    public Page<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
                .findByRecipient_AccountIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::mapToResponse);
    }
    
    @Override
    public List<NotificationResponse> getActionableNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository
                .findByRecipient_AccountIdAndActionRequiredTrueAndActionCompletedFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipient_AccountIdAndIsReadFalse(userId);
    }
    
    @Override
    public Long getActionableCount(UUID userId) {
        return notificationRepository
                .countByRecipient_AccountIdAndActionRequiredTrueAndActionCompletedFalse(userId);
    }
    
    // ========== Action methods ==========
    
    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.markAsRead(notificationId);
    }
    
    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByRecipientId(userId);
    }
    
    @Override
    @Transactional
    public void acceptCustomRequest(UUID notificationId, UUID artisanId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        
        // Validate
        if (!notification.getRecipient().getAccountId().equals(artisanId)) {
            throw new BadRequestException("Not authorized");
        }
        
        if (notification.getType() != NotificationType.NEW_CUSTOM_REQUEST) {
            throw new BadRequestException("Invalid notification type");
        }
        
        if (notification.getActionCompleted()) {
            throw new BadRequestException("Already responded");
        }
        
        // Mark action as completed
        notification.setActionCompleted(true);
        notification.setActionCompletedAt(LocalDateTime.now());
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Update CustomRequest status
        UUID requestId = notification.getRelatedEntityId();
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        
        request.setStatus(CustomRequestStatus.NEGOTIATING);
        customRequestRepository.save(request);
        
        log.info("Artisan {} accepted custom request {}", artisanId, requestId);
    }
    
    @Override
    @Transactional
    public void rejectCustomRequest(UUID notificationId, UUID artisanId, String reason) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        
        // Validate
        if (!notification.getRecipient().getAccountId().equals(artisanId)) {
            throw new BadRequestException("Not authorized");
        }
        
        if (notification.getType() != NotificationType.NEW_CUSTOM_REQUEST) {
            throw new BadRequestException("Invalid notification type");
        }
        
        if (notification.getActionCompleted()) {
            throw new BadRequestException("Already responded");
        }
        
        // Mark as completed (rejected)
        notification.setActionCompleted(true);
        notification.setActionCompletedAt(LocalDateTime.now());
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        
        // Store rejection reason in metadata (simple string format)
        String metadata = notification.getMetadata() + ";rejected=true;rejectionReason=" + reason;
        notification.setMetadata(metadata);
        
        notificationRepository.save(notification);
        
        log.info("Artisan {} rejected custom request {}", artisanId, notification.getRelatedEntityId());
    }
    
    @Override
    @Transactional
    public void completeNotificationAction(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        
        if (!notification.getRecipient().getAccountId().equals(userId)) {
            throw new BadRequestException("Not authorized");
        }
        
        notification.setActionCompleted(true);
        notification.setActionCompletedAt(LocalDateTime.now());
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
    }
    
    // ========== Helper methods ==========
    
    private void sendRealTimeNotification(UUID userId, String topic, NotificationResponse response) {
        String destination = String.format("/topic/user/%s%s", userId, topic);
        messagingTemplate.convertAndSend(destination, response);
        log.debug("Sent real-time notification to {}", destination);
    }
    
    private NotificationResponse mapToResponse(Notification notification) {
        // Parse simple metadata (key=value;key=value format)
        Map<String, Object> metadata = null;
        if (notification.getMetadata() != null && !notification.getMetadata().isEmpty()) {
            metadata = new HashMap<>();
            String[] pairs = notification.getMetadata().split(";");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    metadata.put(keyValue[0], keyValue[1]);
                }
            }
        }
        
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .actionType(notification.getActionType())
                .actionRequired(notification.getActionRequired())
                .actionCompleted(notification.getActionCompleted())
                .actionCompletedAt(notification.getActionCompletedAt())
                .metadata(metadata)
                .priority(notification.getPriority())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
