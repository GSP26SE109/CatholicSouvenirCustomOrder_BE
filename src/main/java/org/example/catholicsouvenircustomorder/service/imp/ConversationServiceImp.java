package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.response.ConversationResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.repository.ConversationRepository;
import org.example.catholicsouvenircustomorder.repository.CustomRequestRepository;
import org.example.catholicsouvenircustomorder.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImp implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final CustomRequestRepository customRequestRepository;
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;

    @Override
    @Transactional
    public ConversationResponse startConversation(UUID requestId, UUID artisanId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        // Validate request status - must be OPEN
        if (request.getStatus() != CustomRequestStatus.OPEN) {
            throw new IllegalArgumentException("Request is not open for conversations. Current status: " + request.getStatus());
        }
        
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan not found"));
        
        // Check if conversation already exists
        if (conversationRepository.existsByRequestAndArtisan(request, artisan)) {
            // Return existing conversation instead of throwing error
            Conversation existing = conversationRepository.findByRequestAndArtisan(request, artisan)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
            return mapToResponse(existing);
        }
        
        // Create new conversation
        Account customer = request.getCustomer();
        
        Conversation conversation = new Conversation();
        conversation.setRequest(request);
        conversation.setCustomer(customer);
        conversation.setArtisan(artisan);
        
        Conversation saved = conversationRepository.save(conversation);
        
        // TODO: Send notification to customer
        // notificationService.notifyCustomer(customer, "Nghệ nhân " + artisan.getAccount().getFullName() + " quan tâm đến yêu cầu của bạn");
        
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public Conversation getOrCreateConversation(UUID requestId, UUID customerId, UUID artisanId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan not found"));
        
        // Check if conversation already exists
        return conversationRepository.findByRequestAndArtisan(request, artisan)
                .orElseGet(() -> {
                    // Create new conversation
                    Account customer = accountRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
                    
                    Conversation conversation = new Conversation();
                    conversation.setRequest(request);
                    conversation.setCustomer(customer);
                    conversation.setArtisan(artisan);
                    
                    return conversationRepository.save(conversation);
                });
    }

    @Override
    public List<ConversationResponse> getCustomerConversations(UUID requestId, UUID customerId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        // Verify customer owns this request
        if (!request.getCustomer().getAccountId().equals(customerId)) {
            throw new IllegalArgumentException("Customer does not own this request");
        }
        
        List<Conversation> conversations = conversationRepository.findByRequestOrderByUpdatedAtDesc(request);
        
        return conversations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationResponse> getArtisanConversations(UUID artisanId) {
        List<Conversation> conversations = conversationRepository.findByArtisanIdOrderByUpdatedAtDesc(artisanId);
        
        return conversations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Conversation getConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    @Override
    public boolean conversationExists(UUID requestId, UUID artisanId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan not found"));
        
        return conversationRepository.existsByRequestAndArtisan(request, artisan);
    }

    private ConversationResponse mapToResponse(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setConversationId(conversation.getConversationId());
        response.setRequestId(conversation.getRequest().getRequestId());
        response.setRequestTitle(conversation.getRequest().getDescription());
        response.setCustomerId(conversation.getCustomer().getAccountId());
        response.setCustomerName(conversation.getCustomer().getFullName());
        response.setArtisanId(conversation.getArtisan().getArtisanUuid());
        response.setArtisanName(conversation.getArtisan().getAccount().getFullName());
        
        // Don't load messages here to avoid N+1 problem
        // Messages should be loaded separately via ChatService
        response.setLastMessage(null);
        response.setLastMessageTime(null);
        response.setUnreadCount(0);
        
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());
        
        return response;
    }
}
