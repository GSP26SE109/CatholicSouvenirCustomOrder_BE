package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.MessageType;

import java.util.UUID;

@Data
public class SendMessageRequest {
    
    @NotNull(message = "Request ID is required")
    private UUID requestId;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private MessageType messageType = MessageType.TEXT;
}
