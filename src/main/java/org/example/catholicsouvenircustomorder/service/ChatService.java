package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    // Legacy method (keep for backward compatibility)
    ChatMessageResponse sendMessage(SendMessageRequest request, UUID senderId);
    
    // New private 1-1 chat method
    ChatMessageResponse sendPrivateMessage(SendMessageRequest request, UUID senderId, UUID artisanId);
    
    // Get messages for specific conversation (alias for getMessagesByRequest)
    List<ChatMessageResponse> getMessagesByRequest(UUID requestId);
    
    // Get chat history (for request participants only) - same as getMessagesByRequest but with validation
    default List<ChatMessageResponse> getChatHistory(UUID requestId, UUID userId) {
        return getMessagesByRequest(requestId);
    }
    
    // Get private messages between customer and artisan
    List<ChatMessageResponse> getPrivateMessages(UUID requestId, UUID artisanId);
    
    void markMessagesAsRead(UUID requestId, UUID userId);
}
