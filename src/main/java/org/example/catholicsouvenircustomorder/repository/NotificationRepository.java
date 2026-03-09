package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Notification;
import org.example.catholicsouvenircustomorder.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    // Basic queries
    Page<Notification> findByRecipient_AccountIdOrderByCreatedAtDesc(
        UUID recipientId, Pageable pageable);
    
    Page<Notification> findByRecipient_AccountIdAndIsReadFalseOrderByCreatedAtDesc(
        UUID recipientId, Pageable pageable);
    
    Page<Notification> findByRecipient_AccountIdAndTypeOrderByCreatedAtDesc(
        UUID recipientId, NotificationType type, Pageable pageable);
    
    // Actionable notifications
    List<Notification> findByRecipient_AccountIdAndActionRequiredTrueAndActionCompletedFalseOrderByCreatedAtDesc(
        UUID recipientId);
    
    // Counts
    Long countByRecipient_AccountIdAndIsReadFalse(UUID recipientId);
    
    Long countByRecipient_AccountIdAndActionRequiredTrueAndActionCompletedFalse(
        UUID recipientId);
    
    // Specific queries
    Optional<Notification> findByRecipient_AccountIdAndRelatedEntityIdAndType(
        UUID recipientId, UUID relatedEntityId, NotificationType type);
    
    // Bulk operations
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipient.accountId = :recipientId AND n.isRead = false")
    void markAllAsReadByRecipientId(@Param("recipientId") UUID recipientId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.notificationId = :notificationId")
    void markAsRead(@Param("notificationId") UUID notificationId);
}
