package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;
import org.example.catholicsouvenircustomorder.dto.response.QuotationResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.ChatMessage;
import org.example.catholicsouvenircustomorder.model.CustomRequest;
import org.example.catholicsouvenircustomorder.model.Quotation;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ChatMessageRepository;
import org.example.catholicsouvenircustomorder.repository.CustomRequestRepository;
import org.example.catholicsouvenircustomorder.repository.QuotationRepository;
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
    private final QuotationRepository quotationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(SendMessageRequest request, UUID senderId) {
        CustomRequest customRequest = customRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Custom request not found"));

        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        ChatMessage message = new ChatMessage();
        message.setCustomRequest(customRequest);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());

        if (request.getRelatedQuotationId() != null) {
            Quotation quotation = quotationRepository.findById(request.getRelatedQuotationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));
            message.setRelatedQuotation(quotation);
        }

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

        if (request.getRelatedQuotationId() != null) {
            Quotation quotation = quotationRepository.findById(request.getRelatedQuotationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));
            message.setRelatedQuotation(quotation);
        }

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
    @Transactional
    public void markMessagesAsRead(UUID requestId, UUID userId) {
        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByCustomRequest_RequestIdAndIsRead(requestId, false);
        
        for (ChatMessage message : unreadMessages) {
            if (!message.getSender().getAccountId().equals(userId)) {
                message.setIsRead(true);
            }
        }
        
        chatMessageRepository.saveAll(unreadMessages);
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

        if (message.getRelatedQuotation() != null) {
            QuotationResponse quotationResponse = new QuotationResponse();
            quotationResponse.setQuotationId(message.getRelatedQuotation().getQuotationId());
            quotationResponse.setPrice(message.getRelatedQuotation().getPrice());
            quotationResponse.setNotes(message.getRelatedQuotation().getNotes());
            response.setRelatedQuotation(quotationResponse);
        }

        return response;
    }
}
