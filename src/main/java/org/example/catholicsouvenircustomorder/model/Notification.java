package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "notifications")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;
    
    // ========== WHO ==========
    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    private Account recipient;
    
    // ========== WHAT ==========
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, length = 1000)
    private String message;
    
    // ========== WHERE (Related Entity) ==========
    private UUID relatedEntityId;
    
    @Enumerated(EnumType.STRING)
    private RelatedEntityType relatedEntityType;
    
    // ========== STATUS ==========
    @Column(nullable = false)
    private Boolean isRead = false;
    
    private LocalDateTime readAt;
    
    // ========== ACTION (For actionable notifications) ==========
    @Enumerated(EnumType.STRING)
    private NotificationAction actionType;
    
    @Column(nullable = false)
    private Boolean actionRequired = false;
    
    @Column(nullable = false)
    private Boolean actionCompleted = false;
    
    private LocalDateTime actionCompletedAt;
    
    // ========== METADATA ==========
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority = NotificationPriority.NORMAL;
    
    // ========== TIMESTAMPS ==========
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
