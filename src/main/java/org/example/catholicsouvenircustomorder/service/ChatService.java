package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    
    /**
     * Send message in a conversation.
     * Supports both WebSocket and REST API.
     */
    ChatMessageResponse sendMessage(SendMessageRequest request, UUID senderId);
    
    /**
     * Get messages in a conversation with pagination.
     * Validates user is participant in the conversation.
     */
    Page<ChatMessageResponse> getConversationMessages(UUID conversationId, UUID userId, Pageable pageable);
    
    /**
     * Get all conversations for a user (customer or artisan).
     * Returns latest message from each conversation.
     */
    List<ChatMessageResponse> getUserConversations(UUID userId);
    
    /**
     * Mark messages as read in a conversation.
     * Only marks messages where user is NOT the sender.
     */
    void markMessagesAsRead(UUID conversationId, UUID userId);
    
    /**
     * Get total unread message count for user.
     */
    Long getUnreadMessageCount(UUID userId);
    
    /**
     * Get unread message count for specific conversation.
     */
    Long getUnreadMessageCount(UUID conversationId, UUID userId);
}
