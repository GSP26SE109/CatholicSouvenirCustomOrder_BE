package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.ChatMessage;
import org.example.catholicsouvenircustomorder.model.CustomRequest;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ChatMessageRepository;
import org.example.catholicsouvenircustomorder.repository.CustomRequestRepository;
import org.example.catholicsouvenircustomorder.service.ChatService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImp implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final CustomRequestRepository customRequestRepository;
    private final AccountRepository accountRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(SendMessageRequest request, UUID senderId) {
        CustomRequest customRequest = customRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));

        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        // Validate sender is customer or selected artisan
        validateSenderAuthorization(customRequest, senderId);

        ChatMessage message = new ChatMessage();
        message.setCustomRequest(customRequest);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());

        message = chatMessageRepository.save(message);

        // Broadcast message to all participants via WebSocket
        ChatMessageResponse response = mapToResponse(message);
        messagingTemplate.convertAndSend(
            "/topic/chat/" + customRequest.getRequestId(),
            response
        );

        return response;
    }

    @Override
    @Transactional
    public ChatMessageResponse sendPrivateMessage(SendMessageRequest request, UUID senderId, UUID artisanId) {
        CustomRequest customRequest = customRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));

        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        Account artisan = accountRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan not found"));

        // Validate sender is customer or selected artisan
        validateSenderAuthorization(customRequest, senderId);

        // Determine receiver based on sender
        UUID receiverId = sender.getAccountId().equals(artisanId) 
            ? customRequest.getCustomer().getAccountId() 
            : artisanId;

        ChatMessage message = new ChatMessage();
        message.setCustomRequest(customRequest);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        
        // Set receiver for private chat
        Account receiver = accountRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        message.setReceiver(receiver);

        message = chatMessageRepository.save(message);

        // Broadcast to PRIVATE channel: /topic/chat/{requestId}/{artisanId}
        ChatMessageResponse response = mapToResponse(message);
        messagingTemplate.convertAndSend(
            "/topic/chat/" + customRequest.getRequestId() + "/" + artisanId,
            response
        );

        return response;
    }

    @Override
    public List<ChatMessageResponse> getPrivateMessages(UUID requestId, UUID artisanId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));
        
        // Get messages where either:
        // - Sender is artisan OR receiver is artisan
        // - For this specific request
        return chatMessageRepository.findByCustomRequest_RequestIdOrderBySentAtAsc(requestId)
                .stream()
                .filter(msg -> {
                    UUID senderId = msg.getSender().getAccountId();
                    UUID receiverId = msg.getReceiver() != null ? msg.getReceiver().getAccountId() : null;
                    
                    // Include message if artisan is sender or receiver
                    return senderId.equals(artisanId) || artisanId.equals(receiverId);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageResponse> getMessagesByRequest(UUID requestId) {
        return chatMessageRepository.findByCustomRequest_RequestIdOrderBySentAtAsc(requestId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageResponse> getChatHistory(UUID requestId, UUID userId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));
        
        // Validate user is participant (customer or selected artisan)
        validateParticipantAuthorization(customRequest, userId);
        
        return getMessagesByRequest(requestId);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(UUID requestId, UUID userId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));
        
        // Validate user is participant (customer or selected artisan)
        validateParticipantAuthorization(customRequest, userId);
        
        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByCustomRequest_RequestIdAndIsRead(requestId, false);
        
        for (ChatMessage message : unreadMessages) {
            if (!message.getSender().getAccountId().equals(userId)) {
                message.setIsRead(true);
            }
        }
        
        chatMessageRepository.saveAll(unreadMessages);
    }

    /**
     * Validates that the sender is authorized to send messages in this request
     * (must be customer or selected artisan)
     */
    private void validateSenderAuthorization(CustomRequest customRequest, UUID senderId) {
        boolean isCustomer = customRequest.getCustomer().getAccountId().equals(senderId);
        boolean isSelectedArtisan = customRequest.getSelectedArtisan() != null 
            && customRequest.getSelectedArtisan().getArtisanUuid().equals(senderId);
        
        // For template-based requests, check if sender is the template owner
        boolean isTemplateOwner = customRequest.getTemplate() != null 
            && customRequest.getTemplate().getArtisan().getArtisanUuid().equals(senderId);
        
        if (!isCustomer && !isSelectedArtisan && !isTemplateOwner) {
            throw new ResourceNotFoundException("Unauthorized: You are not a participant in this conversation");
        }
    }

    /**
     * Validates that the user is authorized to view/interact with messages
     * (must be customer or selected artisan)
     */
    private void validateParticipantAuthorization(CustomRequest customRequest, UUID userId) {
        boolean isCustomer = customRequest.getCustomer().getAccountId().equals(userId);
        boolean isSelectedArtisan = customRequest.getSelectedArtisan() != null 
            && customRequest.getSelectedArtisan().getArtisanUuid().equals(userId);
        
        // For template-based requests, check if user is the template owner
        boolean isTemplateOwner = customRequest.getTemplate() != null 
            && customRequest.getTemplate().getArtisan().getArtisanUuid().equals(userId);
        
        if (!isCustomer && !isSelectedArtisan && !isTemplateOwner) {
            throw new ResourceNotFoundException("Unauthorized: You are not a participant in this conversation");
        }
    }

    private ChatMessageResponse mapToResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setRequestId(message.getCustomRequest().getRequestId());
        response.setSenderId(message.getSender().getAccountId());
        response.setSenderName(message.getSender().getFullName());
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setSentAt(message.getSentAt());
        response.setIsRead(message.getIsRead());

        return response;
    }
}
