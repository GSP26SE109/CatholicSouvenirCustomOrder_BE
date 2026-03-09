package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByCustomRequest_RequestIdOrderBySentAtAsc(UUID requestId);
    List<ChatMessage> findByCustomRequest_RequestIdAndIsRead(UUID requestId, Boolean isRead);
}
