package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ConversationResponse {
    private UUID conversationId;
    private UUID requestId;
    private String requestTitle;
    private UUID customerId;
    private String customerName;
    private UUID artisanId;
    private String artisanName;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
