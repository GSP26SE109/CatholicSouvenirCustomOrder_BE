package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.ConversationResponse;
import org.example.catholicsouvenircustomorder.model.Conversation;

import java.util.List;
import java.util.UUID;

public interface ConversationService {
    
    // Artisan starts conversation with customer (IMPORTANT: First step in flow)
    ConversationResponse startConversation(UUID requestId, UUID artisanId);
    
    // Get or create conversation between customer and artisan for a request
    Conversation getOrCreateConversation(UUID requestId, UUID customerId, UUID artisanId);
    
    // Get all conversations for a customer's request
    List<ConversationResponse> getCustomerConversations(UUID requestId, UUID customerId);
    
    // Get all conversations for an artisan
    List<ConversationResponse> getArtisanConversations(UUID artisanId);
    
    // Get specific conversation
    Conversation getConversation(UUID conversationId);
    
    // Check if conversation exists
    boolean conversationExists(UUID requestId, UUID artisanId);
}
