package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
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
        Account artisan = accountRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Artisan not found"));
        
        String metadata = String.format("customerName=%s;description=%s", 
            customerName, description);
        
        Notification notification = new Notification();
        notification.setRecipient(artisan);
        notification.setType(NotificationType.NEW_CUSTOM_REQUEST);
        notification.setTitle("New Custom Order Request");
        notification.setMessage("You have a new custom order request from " + customerName);
        notification.setRelatedEntityId(requestId);
        notification.setRelatedEntityType(RelatedEntityType.CUSTOM_REQUEST);
        notification.setActionType(NotificationAction.ACCEPT_REQUEST);
        notification.setActionRequired(true);
        notification.setActionCompleted(false);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(artisanId, "/notifications", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfRequestAcceptance(UUID customerId, UUID requestId, String artisanName) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        String metadata = String.format("artisanName=%s", artisanName);
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.REQUEST_ACCEPTED);
        notification.setTitle("Request Accepted");
        notification.setMessage(String.format(
            "Great news! %s has accepted your custom order request. A custom order will be created.", 
            artisanName
        ));
        notification.setRelatedEntityId(requestId);
        notification.setRelatedEntityType(RelatedEntityType.CUSTOM_REQUEST);
        notification.setActionType(NotificationAction.VIEW_ORDER);
        notification.setActionRequired(false);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(customerId, "/request-updates", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfRequestRejection(UUID customerId, UUID requestId, 
                                                String artisanName, String reason) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        String metadata = String.format("artisanName=%s;reason=%s", artisanName, reason);
        
        String message = String.format("%s has declined your custom order request.", artisanName);
        if (reason != null && !reason.trim().isEmpty()) {
            message += " Reason: " + reason;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.REQUEST_REJECTED);
        notification.setTitle("Request Declined");
        notification.setMessage(message);
        notification.setRelatedEntityId(requestId);
        notification.setRelatedEntityType(RelatedEntityType.CUSTOM_REQUEST);
        notification.setActionType(NotificationAction.VIEW_REQUEST);
        notification.setActionRequired(false);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(customerId, "/request-updates", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfNewQuotation(UUID customerId, UUID quotationId, 
                                            String artisanName, Long price) {
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
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfOrderCreated(UUID customerId, UUID orderId, 
                                            Long totalAmount, Integer stagesCount) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        String metadata = String.format("totalAmount=%d;stagesCount=%d", totalAmount, stagesCount);
        
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
    }
    
    @Override
    @Transactional
    public void notifyArtisanOfPayment(UUID artisanId, UUID stageId, String stageName, Long amount) {
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
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfStageCompletion(UUID customerId, UUID stageId, String stageName,
                                               UUID nextStageId, String nextStageName, Long nextAmount) {
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
    }
    
    // ========== Template-Based Flow Notifications ==========
    
    @Override
    @Transactional
    public void notifyArtisanOfPaymentSuccess(UUID artisanId, UUID customOrderId, String customerName, Long amount) {
        Account artisan = accountRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Artisan not found"));
        
        String metadata = String.format("customerName=%s;amount=%d", customerName, amount);
        
        Notification notification = new Notification();
        notification.setRecipient(artisan);
        notification.setType(NotificationType.PAYMENT_RECEIVED);
        notification.setTitle("Payment Received");
        notification.setMessage(String.format(
            "Payment received from %s: %,d VNĐ. You can start production.", 
            customerName, amount
        ));
        notification.setRelatedEntityId(customOrderId);
        notification.setRelatedEntityType(RelatedEntityType.CUSTOM_ORDER);
        notification.setActionType(NotificationAction.VIEW_ORDER);
        notification.setActionRequired(false);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(artisanId, "/payments", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfOrderCompletion(UUID customerId, UUID customOrderId, String artisanName) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        String metadata = String.format("artisanName=%s", artisanName);
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.ORDER_COMPLETED);
        notification.setTitle("Order Completed");
        notification.setMessage(String.format(
            "Great news! %s has completed your custom order. It will be shipped soon.", 
            artisanName
        ));
        notification.setRelatedEntityId(customOrderId);
        notification.setRelatedEntityType(RelatedEntityType.CUSTOM_ORDER);
        notification.setActionType(NotificationAction.VIEW_ORDER);
        notification.setActionRequired(false);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(customerId, "/orders", mapToResponse(notification));
    }
    
    // ========== Request-Based Flow Notifications ==========
    
    @Override
    @Transactional
    public void notifyArtisanOfSelection(UUID artisanId, UUID requestId, String customerName, String description) {
        Account artisan = accountRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Artisan not found"));
        
        String metadata = String.format("customerName=%s;description=%s", customerName, description);
        
        Notification notification = new Notification();
        notification.setRecipient(artisan);
        notification.setType(NotificationType.REQUEST_CONFIRMED);
        notification.setTitle("You've Been Selected!");
        notification.setMessage(String.format(
            "%s has selected you for their custom request. Start negotiating to create an order.", 
            customerName
        ));
        notification.setRelatedEntityId(requestId);
        notification.setRelatedEntityType(RelatedEntityType.CUSTOM_REQUEST);
        notification.setActionType(NotificationAction.VIEW_REQUEST);
        notification.setActionRequired(true);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(artisanId, "/request-updates", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfOrderCreatedWithStages(UUID customerId, UUID orderId, Long totalAmount, Integer stagesCount) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        String metadata = String.format("totalAmount=%d;stagesCount=%d", totalAmount, stagesCount);
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("Order Created with Stages");
        notification.setMessage(String.format(
            "Your custom order has been created with %d payment stages. Total: %,d VNĐ. Please pay Stage 1 to start production.", 
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
    }
    
    @Override
    @Transactional
    public void notifyCustomerOfStagePaymentRequired(UUID customerId, UUID stageId, String stageName, Long amount) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        String metadata = String.format("stageName=%s;amount=%d", stageName, amount);
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.PAYMENT_PENDING);
        notification.setTitle("Stage Payment Required");
        notification.setMessage(String.format(
            "Please pay for %s (%,d VNĐ) to continue production.", 
            stageName, amount
        ));
        notification.setRelatedEntityId(stageId);
        notification.setRelatedEntityType(RelatedEntityType.STAGE);
        notification.setActionType(NotificationAction.PAY_STAGE);
        notification.setActionRequired(true);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(customerId, "/stage-updates", mapToResponse(notification));
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
        
        if (!notification.getRecipient().getAccountId().equals(artisanId)) {
            throw new BadRequestException("Not authorized");
        }
        
        if (notification.getType() != NotificationType.NEW_CUSTOM_REQUEST) {
            throw new BadRequestException("Invalid notification type");
        }
        
        if (notification.getActionCompleted()) {
            throw new BadRequestException("Already responded");
        }
        
        notification.setActionCompleted(true);
        notification.setActionCompletedAt(LocalDateTime.now());
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        UUID requestId = notification.getRelatedEntityId();
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        
        request.setStatus(CustomRequestStatus.ACCEPTED);
        customRequestRepository.save(request);
    }
    
    @Override
    @Transactional
    public void rejectCustomRequest(UUID notificationId, UUID artisanId, String reason) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        
        if (!notification.getRecipient().getAccountId().equals(artisanId)) {
            throw new BadRequestException("Not authorized");
        }
        
        if (notification.getType() != NotificationType.NEW_CUSTOM_REQUEST) {
            throw new BadRequestException("Invalid notification type");
        }
        
        if (notification.getActionCompleted()) {
            throw new BadRequestException("Already responded");
        }
        
        notification.setActionCompleted(true);
        notification.setActionCompletedAt(LocalDateTime.now());
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        
        String metadata = notification.getMetadata() + ";rejected=true;rejectionReason=" + reason;
        notification.setMetadata(metadata);
        notificationRepository.save(notification);
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
    }
    
    private NotificationResponse mapToResponse(Notification notification) {
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
