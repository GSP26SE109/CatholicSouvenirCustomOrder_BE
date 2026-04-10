package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ChatMessageRepository;
import org.example.catholicsouvenircustomorder.repository.ConversationRepository;
import org.example.catholicsouvenircustomorder.repository.CustomRequestRepository;
import org.example.catholicsouvenircustomorder.service.ChatService;
import org.example.catholicsouvenircustomorder.service.ConversationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImp implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final CustomRequestRepository customRequestRepository;
    private final AccountRepository accountRepository;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(SendMessageRequest request, UUID senderId) {
        // Legacy method - not recommended, use sendPrivateMessage instead
        throw new UnsupportedOperationException("Use sendPrivateMessage instead");
    }

    @Override
    @Transactional
    public ChatMessageResponse sendPrivateMessage(SendMessageRequest request, UUID senderId, UUID artisanId) {
        CustomRequest customRequest = customRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        // Get or create conversation
        Conversation conversation = conversationService.getOrCreateConversation(
                request.getRequestId(), 
                customRequest.getCustomer().getAccountId(), 
                artisanId
        );
        
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
        
        // Broadcast to private conversation topic
        ChatMessageResponse response = mapToResponse(savedMessage);
        String destination = String.format("/topic/chat/%s/%s", request.getRequestId(), artisanId);
        messagingTemplate.convertAndSend(destination, response);
        
        return response;
    }

    @Override
    public List<ChatMessageResponse> getPrivateMessages(UUID requestId, UUID artisanId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        // Find conversation
        Conversation conversation = conversationService.getOrCreateConversation(
                requestId, 
                customRequest.getCustomer().getAccountId(), 
                artisanId
        );
        
        List<ChatMessage> messages = chatMessageRepository.findByConversationOrderBySentAtAsc(conversation);
        
        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageResponse> getMessagesByRequest(UUID requestId) {
        // Get all messages for a request (across all conversations)
        List<ChatMessage> messages = chatMessageRepository.findByRequestIdOrderBySentAtAsc(requestId);
        
        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageResponse> getChatHistory(UUID requestId, UUID userId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        // Verify user is participant (customer or artisan)
        boolean isCustomer = customRequest.getCustomer().getAccountId().equals(userId);
        boolean isArtisan = customRequest.getSelectedArtisan() != null && 
                           customRequest.getSelectedArtisan().getAccount().getAccountId().equals(userId);
        
        if (!isCustomer && !isArtisan) {
            throw new IllegalArgumentException("User is not a participant in this request");
        }
        
        return getMessagesByRequest(requestId);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(UUID requestId, UUID userId) {
        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByRequestIdAndIsRead(requestId, false);
        
        // Mark messages as read where the user is NOT the sender
        unreadMessages.stream()
                .filter(msg -> !msg.getSender().getAccountId().equals(userId))
                .forEach(msg -> msg.setIsRead(true));
        
        chatMessageRepository.saveAll(unreadMessages);
    }

    private ChatMessageResponse mapToResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(message.getMessageId());
        
        if (message.getConversation() != null) {
            response.setRequestId(message.getConversation().getRequest().getRequestId());
        }
        
        response.setSenderId(message.getSender().getAccountId());
        response.setSenderName(message.getSender().getFullName());
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setSentAt(message.getSentAt());
        response.setIsRead(message.getIsRead());
        
        return response;
    }
}
