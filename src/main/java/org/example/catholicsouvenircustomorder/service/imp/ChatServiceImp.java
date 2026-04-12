package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ChatMessageRepository;
import org.example.catholicsouvenircustomorder.repository.ConversationRepository;
import org.example.catholicsouvenircustomorder.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImp implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final AccountRepository accountRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(SendMessageRequest request, UUID senderId) {
        // Validate conversation exists and user is participant
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        
        // Verify sender is participant in conversation
        boolean isParticipant = conversation.getCustomer().getAccountId().equals(senderId) ||
                               conversation.getArtisan().getAccount().getAccountId().equals(senderId);
        
        if (!isParticipant) {
            throw new IllegalArgumentException("User is not a participant in this conversation");
        }
        
        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        // Create message
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false);
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Update conversation timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        // Broadcast to conversation topic via WebSocket
        ChatMessageResponse response = mapToResponse(savedMessage);
        String destination = String.format("/topic/chat/%s", request.getConversationId());
        messagingTemplate.convertAndSend(destination, response);
        
        return response;
    }

    @Override
    public Page<ChatMessageResponse> getConversationMessages(UUID conversationId, UUID userId, Pageable pageable) {
        // Validate conversation exists and user is participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        
        boolean isParticipant = conversation.getCustomer().getAccountId().equals(userId) ||
                               conversation.getArtisan().getAccount().getAccountId().equals(userId);
        
        if (!isParticipant) {
            throw new IllegalArgumentException("User is not a participant in this conversation");
        }
        
        // Get messages with pagination (newest first, then reverse for display)
        List<ChatMessage> allMessages = chatMessageRepository.findByConversationOrderBySentAtAsc(conversation);
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allMessages.size());
        
        List<ChatMessageResponse> messageResponses = allMessages.subList(start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(messageResponses, pageable, allMessages.size());
    }

    @Override
    public List<ChatMessageResponse> getUserConversations(UUID userId) {
        // Get all conversations where user is participant
        List<Conversation> conversations = conversationRepository.findByCustomerAccountIdOrArtisanAccountAccountId(userId, userId);
        
        return conversations.stream()
                .map(conversation -> {
                    // Get latest message from each conversation
                    Optional<ChatMessage> latestMessage = chatMessageRepository
                            .findByConversationOrderBySentAtAsc(conversation)
                            .stream()
                            .reduce((first, second) -> second);
                    
                    if (latestMessage.isPresent()) {
                        ChatMessageResponse response = mapToResponse(latestMessage.get());
                        
                        // Add conversation info
                        if (conversation.getCustomer().getAccountId().equals(userId)) {
                            // User is customer, show artisan info
                            response.setOtherParticipantId(conversation.getArtisan().getAccount().getAccountId());
                            response.setOtherParticipantName(conversation.getArtisan().getAccount().getFullName());
                        } else {
                            // User is artisan, show customer info
                            response.setOtherParticipantId(conversation.getCustomer().getAccountId());
                            response.setOtherParticipantName(conversation.getCustomer().getFullName());
                        }
                        
                        return response;
                    }
                    return null;
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessagesAsRead(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        
        // Mark messages as read where user is NOT the sender
        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByConversationAndIsRead(conversation, false).stream()
                .filter(msg -> !msg.getSender().getAccountId().equals(userId))
                .collect(Collectors.toList());
        
        unreadMessages.forEach(msg -> msg.setIsRead(true));
        chatMessageRepository.saveAll(unreadMessages);
    }

    @Override
    public Long getUnreadMessageCount(UUID userId) {
        // Get all conversations for user
        List<Conversation> conversations = conversationRepository.findByCustomerAccountIdOrArtisanAccountAccountId(userId, userId);
        
        return conversations.stream()
                .mapToLong(conv -> chatMessageRepository.countUnreadByConversationAndUser(conv, userId))
                .sum();
    }

    @Override
    public Long getUnreadMessageCount(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        
        return chatMessageRepository.countUnreadByConversationAndUser(conversation, userId);
    }

    private ChatMessageResponse mapToResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setConversationId(message.getConversation().getConversationId());
        response.setSenderId(message.getSender().getAccountId());
        response.setSenderName(message.getSender().getFullName());
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setSentAt(message.getSentAt());
        response.setIsRead(message.getIsRead());
        
        return response;
    }
}
