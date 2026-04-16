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
        notification.setTitle("Yêu cầu đặt hàng mới");
        notification.setMessage("Bạn có yêu cầu đặt hàng tùy chỉnh mới từ " + customerName);
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
        notification.setTitle("Yêu cầu được chấp nhận");
        notification.setMessage(String.format(
            "Tin vui! Nghệ nhân %s đã chấp nhận yêu cầu của bạn. Đơn hàng tùy chỉnh sẽ được tạo.", 
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
        
        String message = String.format("Nghệ nhân %s đã từ chối yêu cầu của bạn.", artisanName);
        if (reason != null && !reason.trim().isEmpty()) {
            message += " Lý do: " + reason;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.REQUEST_REJECTED);
        notification.setTitle("Yêu cầu bị từ chối");
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
    public void notifyCustomerOfOrderCreated(UUID customerId, UUID orderId, 
                                            Long totalAmount, Integer stagesCount) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        String metadata = String.format("totalAmount=%d;stagesCount=%d", totalAmount, stagesCount);
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("Đơn hàng đã tạo");
        notification.setMessage(String.format(
            "Đơn hàng tùy chỉnh của bạn đã được tạo với %d giai đoạn. Tổng: %,d VNĐ. Vui lòng thanh toán Giai đoạn 1 để bắt đầu.", 
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
        notification.setTitle("Đã nhận thanh toán");
        notification.setMessage(String.format(
            "Đã nhận thanh toán cho %s: %,d VNĐ. Bạn có thể bắt đầu làm việc.", 
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
                "%s đã hoàn thành! Vui lòng thanh toán %s (%,d VNĐ) để tiếp tục.", 
                stageName, nextStageName, nextAmount
            );
            metadata += String.format(";nextStageId=%s;nextStageName=%s;nextAmount=%d",
                nextStageId, nextStageName, nextAmount);
        } else {
            message = String.format(
                "%s đã hoàn thành! Đơn hàng của bạn đã xong và sẽ được giao sớm.", 
                stageName
            );
        }
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.STAGE_COMPLETED);
        notification.setTitle("Giai đoạn hoàn thành");
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
        notification.setTitle("Đã nhận thanh toán");
        notification.setMessage(String.format(
            "Đã nhận thanh toán từ %s: %,d VNĐ. Bạn có thể bắt đầu sản xuất.", 
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
        notification.setTitle("Đơn hàng hoàn thành");
        notification.setMessage(String.format(
            "Tin vui! Nghệ nhân %s đã hoàn thành đơn hàng của bạn. Sản phẩm sẽ được giao sớm.", 
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
        notification.setTitle("Bạn đã được chọn!");
        notification.setMessage(String.format(
            "%s đã chọn bạn cho yêu cầu tùy chỉnh của họ. Bắt đầu thương lượng để tạo đơn hàng.", 
            customerName
        ));
        notification.setRelatedEntityId(requestId);
        notification.setRelatedEntityType(RelatedEntityType.CUSTOM_REQUEST);
        notification.setActionType(NotificationAction.VIEW_REQUEST);
        notification.setActionRequired(true);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(artisanId, "/notifications", mapToResponse(notification));
    }
    
    // ========== Conversation & Chat Notifications ==========
    
    @Override
    @Transactional
    public void notifyCustomerOfNewConversation(UUID customerId, UUID conversationId, String artisanName, UUID requestId) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        
        String metadata = String.format("artisanName=%s;requestId=%s", artisanName, requestId);
        
        Notification notification = new Notification();
        notification.setRecipient(customer);
        notification.setType(NotificationType.NEW_CONVERSATION);
        notification.setTitle("Nghệ nhân quan tâm");
        notification.setMessage(String.format(
            "Nghệ nhân %s quan tâm đến yêu cầu của bạn", 
            artisanName
        ));
        notification.setRelatedEntityId(conversationId);
        notification.setRelatedEntityType(RelatedEntityType.CONVERSATION);
        notification.setActionType(NotificationAction.VIEW_CONVERSATION);
        notification.setActionRequired(false);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(customerId, "/notifications", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void notifyUserOfNewMessage(UUID recipientId, UUID conversationId, String senderName, String messagePreview) {
        Account recipient = accountRepository.findById(recipientId)
                .orElseThrow(() -> new NotFoundException("Recipient not found"));
        
        String metadata = String.format("senderName=%s;conversationId=%s", senderName, conversationId);
        
        // Truncate message preview to 100 characters
        String preview = messagePreview.length() > 100 
            ? messagePreview.substring(0, 100) + "..." 
            : messagePreview;
        
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.NEW_MESSAGE);
        notification.setTitle("Tin nhắn mới");
        notification.setMessage(String.format(
            "%s: %s", 
            senderName, preview
        ));
        notification.setRelatedEntityId(conversationId);
        notification.setRelatedEntityType(RelatedEntityType.CONVERSATION);
        notification.setActionType(NotificationAction.VIEW_CONVERSATION);
        notification.setActionRequired(false);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(recipientId, "/notifications", mapToResponse(notification));
    }
    
    // ========== Withdrawal Notifications ==========
    
    @Override
    @Transactional
    public void notifyAdminOfWithdrawalRequest(UUID withdrawalId, String artisanName, Long amount) {
        // Get all admin accounts
        List<Account> admins = accountRepository.findByRole_Name("ADMIN");
        
        String metadata = String.format("artisanName=%s;amount=%d", artisanName, amount);
        
        for (Account admin : admins) {
            Notification notification = new Notification();
            notification.setRecipient(admin);
            notification.setType(NotificationType.WITHDRAWAL_REQUESTED);
            notification.setTitle("Yêu cầu rút tiền mới");
            notification.setMessage(String.format(
                "Nghệ nhân %s đã tạo yêu cầu rút tiền %,d VNĐ. Vui lòng xem xét và phê duyệt.", 
                artisanName, amount
            ));
            notification.setRelatedEntityId(withdrawalId);
            notification.setRelatedEntityType(RelatedEntityType.WITHDRAWAL_REQUEST);
            notification.setActionType(NotificationAction.VIEW_REQUEST);
            notification.setActionRequired(true);
            notification.setActionCompleted(false);
            notification.setPriority(NotificationPriority.HIGH);
            notification.setMetadata(metadata);
            
            notification = notificationRepository.save(notification);
            sendRealTimeNotification(admin.getAccountId(), "/notifications", mapToResponse(notification));
        }
    }
    
    @Override
    @Transactional
    public void notifyArtisanOfWithdrawalApproval(UUID artisanId, UUID withdrawalId, Long amount) {
        Account artisan = accountRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Artisan not found"));
        
        String metadata = String.format("amount=%d", amount);
        
        Notification notification = new Notification();
        notification.setRecipient(artisan);
        notification.setType(NotificationType.WITHDRAWAL_APPROVED);
        notification.setTitle("Yêu cầu rút tiền đã được phê duyệt");
        notification.setMessage(String.format(
            "Yêu cầu rút tiền %,d VNĐ của bạn đã được phê duyệt. Tiền sẽ được chuyển vào tài khoản ngân hàng của bạn trong 1-3 ngày làm việc.", 
            amount
        ));
        notification.setRelatedEntityId(withdrawalId);
        notification.setRelatedEntityType(RelatedEntityType.WITHDRAWAL_REQUEST);
        notification.setActionType(NotificationAction.VIEW_REQUEST);
        notification.setActionRequired(false);
        notification.setPriority(NotificationPriority.HIGH);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(artisanId, "/notifications", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void notifyArtisanOfWithdrawalRejection(UUID artisanId, UUID withdrawalId, Long amount, String reason) {
        Account artisan = accountRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Artisan not found"));
        
        String metadata = String.format("amount=%d;reason=%s", amount, reason);
        
        String message = String.format(
            "Yêu cầu rút tiền %,d VNĐ của bạn đã bị từ chối.", 
            amount
        );
        if (reason != null && !reason.trim().isEmpty()) {
            message += " Lý do: " + reason;
        }
        
        Notification notification = new Notification();
        notification.setRecipient(artisan);
        notification.setType(NotificationType.WITHDRAWAL_REJECTED);
        notification.setTitle("Yêu cầu rút tiền bị từ chối");
        notification.setMessage(message);
        notification.setRelatedEntityId(withdrawalId);
        notification.setRelatedEntityType(RelatedEntityType.WITHDRAWAL_REQUEST);
        notification.setActionType(NotificationAction.VIEW_REQUEST);
        notification.setActionRequired(false);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setMetadata(metadata);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(artisanId, "/notifications", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void notifyAdminOfWithdrawalCancellation(UUID withdrawalId, String artisanName, Long amount) {
        // Get all admin accounts
        List<Account> admins = accountRepository.findByRole_Name("ADMIN");
        
        String metadata = String.format("artisanName=%s;amount=%d", artisanName, amount);
        
        for (Account admin : admins) {
            Notification notification = new Notification();
            notification.setRecipient(admin);
            notification.setType(NotificationType.WITHDRAWAL_CANCELLED);
            notification.setTitle("Yêu cầu rút tiền đã bị hủy");
            notification.setMessage(String.format(
                "Nghệ nhân %s đã hủy yêu cầu rút tiền %,d VNĐ.", 
                artisanName, amount
            ));
            notification.setRelatedEntityId(withdrawalId);
            notification.setRelatedEntityType(RelatedEntityType.WITHDRAWAL_REQUEST);
            notification.setActionType(NotificationAction.VIEW_REQUEST);
            notification.setActionRequired(false);
            notification.setPriority(NotificationPriority.LOW);
            notification.setMetadata(metadata);
            
            notification = notificationRepository.save(notification);
            sendRealTimeNotification(admin.getAccountId(), "/notifications", mapToResponse(notification));
        }
    }
    
    // ========== Core Methods ==========
    
    @Override
    @Transactional
    public void sendNotification(UUID recipientId, NotificationType type, String title, 
                                 String message, UUID relatedEntityId) {
        Account recipient = accountRepository.findById(recipientId)
                .orElseThrow(() -> new NotFoundException("Recipient not found"));
        
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setIsRead(false);
        notification.setPriority(NotificationPriority.NORMAL);
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(recipientId, "/notifications", mapToResponse(notification));
    }
    
    @Override
    @Transactional
    public void broadcastToAllArtisans(NotificationType type, String title, String message, 
                                      UUID relatedEntityId) {
        // Get all artisans
        List<Account> artisans = accountRepository.findByRole_Name("ARTISAN");
        
        for (Account artisan : artisans) {
            try {
                sendNotification(artisan.getAccountId(), type, title, message, relatedEntityId);
            } catch (Exception e) {
                // Log and continue with other artisans
            }
        }
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
        notification.setTitle("Đơn hàng đã tạo với các giai đoạn");
        notification.setMessage(String.format(
            "Đơn hàng tùy chỉnh của bạn đã được tạo với %d giai đoạn thanh toán. Tổng: %,d VNĐ. Vui lòng thanh toán Giai đoạn 1 để bắt đầu sản xuất.", 
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
        notification.setTitle("Cần thanh toán giai đoạn");
        notification.setMessage(String.format(
            "Vui lòng thanh toán cho %s (%,d VNĐ) để tiếp tục sản xuất.", 
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
        
        request.setStatus(CustomRequestStatus.ARTISAN_SELECTED);
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
