package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.ConversationResponse;
import org.example.catholicsouvenircustomorder.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing conversations between customers and artisans.
 * Conversations are created when artisan shows interest in a custom request.
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Artisan starts conversation with customer for a request.
     * This is the FIRST step after customer publishes request.
     */
    @PostMapping("/start")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<ConversationResponse>> startConversation(
            @RequestParam UUID requestId,
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        ConversationResponse conversation = conversationService.startConversation(requestId, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Bắt đầu hội thoại thành công", conversation));
    }
    
    /**
     * Customer views all conversations (interested artisans) for their request.
     */
    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<List<ConversationResponse>>> getRequestConversations(
            @PathVariable UUID requestId,
            Authentication authentication) {
        
        UUID customerId = UUID.fromString(authentication.getName());
        List<ConversationResponse> conversations = conversationService.getCustomerConversations(requestId, customerId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách hội thoại thành công", conversations));
    }
    
    /**
     * Artisan views all their conversations across all requests.
     */
    @GetMapping("/my-conversations")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse<List<ConversationResponse>>> getMyConversations(
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        List<ConversationResponse> conversations = conversationService.getArtisanConversations(artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách hội thoại thành công", conversations));
    }
    
    /**
     * Get conversation detail by ID.
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<BaseResponse<ConversationResponse>> getConversationDetail(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        
        var conversation = conversationService.getConversation(conversationId);
        var response = conversationService.getCustomerConversations(
            conversation.getRequest().getRequestId(), 
            conversation.getCustomer().getAccountId()
        ).stream()
        .filter(c -> c.getConversationId().equals(conversationId))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        return ResponseEntity.ok(BaseResponse.success("Lấy chi tiết hội thoại thành công", response));
    }
}
