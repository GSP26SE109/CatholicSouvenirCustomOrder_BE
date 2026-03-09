package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    // ========== Universal Notification methods ==========
    // Create notifications
    void notifyArtisanOfNewCustomRequest(UUID artisanId, UUID requestId, String customerName, String description);
    void notifyCustomerOfNewQuotation(UUID customerId, UUID quotationId, String artisanName, Long price);
    void notifyCustomerOfOrderCreated(UUID customerId, UUID orderId, Long totalAmount, Integer stagesCount);
    void notifyArtisanOfPayment(UUID artisanId, UUID stageId, String stageName, Long amount);
    void notifyCustomerOfStageCompletion(UUID customerId, UUID stageId, String stageName, 
                                        UUID nextStageId, String nextStageName, Long nextAmount);
    
    // Query notifications
    Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);
    Page<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable);
    List<NotificationResponse> getActionableNotifications(UUID userId);
    Long getUnreadCount(UUID userId);
    Long getActionableCount(UUID userId);
    
    // Actions
    void markAsRead(UUID notificationId);
    void markAllAsRead(UUID userId);
    void acceptCustomRequest(UUID notificationId, UUID artisanId);
    void rejectCustomRequest(UUID notificationId, UUID artisanId, String reason);
    void completeNotificationAction(UUID notificationId, UUID userId);
}
