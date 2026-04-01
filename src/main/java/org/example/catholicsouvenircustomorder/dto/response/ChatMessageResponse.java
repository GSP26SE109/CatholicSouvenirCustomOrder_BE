package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Data;
import org.example.catholicsouvenircustomorder.model.MessageType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ChatMessageResponse {
    private UUID messageId;
    private UUID requestId;
    private UUID senderId;
    private String senderName;
    private String content;
    private MessageType messageType;
    private LocalDateTime sentAt;
    private Boolean isRead;
}
