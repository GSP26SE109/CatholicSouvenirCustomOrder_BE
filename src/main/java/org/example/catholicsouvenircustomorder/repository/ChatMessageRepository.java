package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.ChatMessage;
import org.example.catholicsouvenircustomorder.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    // Get messages by conversation
    List<ChatMessage> findByConversationOrderBySentAtAsc(Conversation conversation);
    
    // Get unread messages for a conversation
    List<ChatMessage> findByConversationAndIsRead(Conversation conversation, boolean isRead);
    
    // Count unread messages for a user in a conversation
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation = :conversation AND m.isRead = false AND m.sender.accountId != :userId")
    long countUnreadByConversationAndUser(@Param("conversation") Conversation conversation, @Param("userId") UUID userId);
    
    // Get all messages for a request (across all conversations)
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.request.requestId = :requestId ORDER BY m.sentAt ASC")
    List<ChatMessage> findByRequestIdOrderBySentAtAsc(@Param("requestId") UUID requestId);
    
    // Get unread messages for a request
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.request.requestId = :requestId AND m.isRead = :isRead")
    List<ChatMessage> findByRequestIdAndIsRead(@Param("requestId") UUID requestId, @Param("isRead") boolean isRead);
}
