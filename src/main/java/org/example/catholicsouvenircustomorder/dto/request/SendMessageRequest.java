package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.MessageType;

import java.util.UUID;

@Data
public class SendMessageRequest {
    
    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;
    
    @NotBlank(message = "Content cannot be empty")
    private String content;
    
    @NotNull(message = "Message type is required")
    private MessageType messageType = MessageType.TEXT;
}
