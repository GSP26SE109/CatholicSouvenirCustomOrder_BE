package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.NotificationResponse;
import org.example.catholicsouvenircustomorder.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    // ========== Core Notification Method ==========
    /**
     * Send notification and broadcast via WebSocket
     */
    void sendNotification(UUID recipientId, NotificationType type, String title, 
                         String message, UUID relatedEntityId);
    
    /**
     * Broadcast to all artisans (for new requests)
     */
    void broadcastToAllArtisans(NotificationType type, String title, String message, 
                                UUID relatedEntityId);
    
    // ========== Specific Notification methods ==========
    void notifyArtisanOfNewCustomRequest(UUID artisanId, UUID requestId, String customerName, String description, String aiConceptImageUrl);
    void notifyCustomerOfRequestAcceptance(UUID customerId, UUID requestId, String artisanName);
    void notifyCustomerOfRequestRejection(UUID customerId, UUID requestId, String artisanName, String reason);
    void notifyCustomerOfOrderCreated(UUID customerId, UUID orderId, Long totalAmount, Integer stagesCount);
    void notifyArtisanOfPayment(UUID artisanId, UUID stageId, String stageName, Long amount);
    void notifyCustomerOfStageCompletion(UUID customerId, UUID stageId, String stageName, 
                                        UUID nextStageId, String nextStageName, Long nextAmount);
    void notifyArtisanOfPaymentSuccess(UUID artisanId, UUID customOrderId, String customerName, Long amount);
    void notifyCustomerOfOrderCompletion(UUID customerId, UUID customOrderId, String artisanName);
    void notifyArtisanOfSelection(UUID artisanId, UUID requestId, String customerName, String description);
    void notifyCustomerOfOrderCreatedWithStages(UUID customerId, UUID orderId, Long totalAmount, Integer stagesCount);
    void notifyCustomerOfStagePaymentRequired(UUID customerId, UUID stageId, String stageName, Long amount);
    void notifyArtisanOfOrderConfirmation(UUID artisanId, UUID orderId, String customerName);
    
    // New: Conversation & Chat notifications
    void notifyCustomerOfNewConversation(UUID customerId, UUID conversationId, String artisanName, UUID requestId);
    void notifyUserOfNewMessage(UUID recipientId, UUID conversationId, String senderName, String messagePreview);
    
    // Withdrawal notifications
    void notifyAdminOfWithdrawalRequest(UUID withdrawalId, String artisanName, Long amount);
    void notifyArtisanOfWithdrawalApproval(UUID artisanId, UUID withdrawalId, Long amount);
    void notifyArtisanOfWithdrawalRejection(UUID artisanId, UUID withdrawalId, Long amount, String reason);
    void notifyAdminOfWithdrawalCancellation(UUID withdrawalId, String artisanName, Long amount);
    
    // Commission notifications
    void notifyAllArtisansCommissionChange(String oldRate, String newRate);
    void notifyArtisanCommissionDeducted(UUID artisanId, UUID orderId, BigDecimal originalAmount, 
                                        BigDecimal commissionAmount, BigDecimal netAmount, 
                                        UUID walletTransactionId);
    
    // Refund notifications
    void notifyCustomerRefundInitiated(UUID customerId, UUID complaintId, BigDecimal amount);
    void notifyCustomerRefundProcessing(UUID customerId, UUID complaintId, BigDecimal amount);
    void notifyCustomerRefundCompleted(UUID customerId, UUID complaintId, BigDecimal amount);
    void notifyCustomerRefundFailed(UUID customerId, UUID complaintId, BigDecimal amount, String reason);
    
    // Cancellation notifications
    void notifyArtisanOfCustomerCancellation(UUID artisanId, UUID orderId, String customerName, 
                                            String reason, BigDecimal grossRefund, 
                                            BigDecimal platformCommission, BigDecimal netRefund);
    void notifyCustomerOfArtisanCancellation(UUID customerId, UUID orderId, String artisanName, 
                                            String reason, BigDecimal netRefund);
    void notifyCustomerOfRefundCompletion(UUID customerId, UUID orderId, BigDecimal netRefund, 
                                         String vnpayTransactionNo);
    void notifyAdminOfInsuranceFundUsage(UUID orderId, UUID artisanId, String artisanName, 
                                        BigDecimal shortfallAmount, String artisanCccd, 
                                        String artisanPhone, String artisanAddress);
    
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
