package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.NotificationAction;
import org.example.catholicsouvenircustomorder.model.NotificationPriority;
import org.example.catholicsouvenircustomorder.model.NotificationType;
import org.example.catholicsouvenircustomorder.model.RelatedEntityType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID notificationId;
    private NotificationType type;
    private String title;
    private String message;
    private UUID relatedEntityId;
    private RelatedEntityType relatedEntityType;
    private Boolean isRead;
    private LocalDateTime readAt;
    private NotificationAction actionType;
    private Boolean actionRequired;
    private Boolean actionCompleted;
    private LocalDateTime actionCompletedAt;
    private Map<String, Object> metadata;
    private NotificationPriority priority;
    private LocalDateTime createdAt;
}
