package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;
import org.example.catholicsouvenircustomorder.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Controller for chat messaging between customers and artisans.
 * Handles sending/receiving messages within conversations.
 * For conversation management, see ConversationController.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ============= WebSocket Endpoints =============
    
    /**
     * WebSocket endpoint for sending messages in real-time.
     * Topic: /topic/messages/{requestId}/{artisanId}
     */
    @MessageMapping("/messages/{requestId}/{artisanId}")
    public void sendMessageViaWebSocket(
            @Payload SendMessageRequest request, 
            @DestinationVariable UUID requestId,
            @DestinationVariable UUID artisanId,
            Principal principal) {
        
        UUID senderId = UUID.fromString(principal.getName());
        request.setRequestId(requestId);
        chatService.sendPrivateMessage(request, senderId, artisanId);
    }

    // ============= REST API - Send Messages =============
    
    /**
     * Send message via REST API (alternative to WebSocket).
     * Use this for reliability or when WebSocket is not available.
     */
    @PostMapping("/api/messages")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<ChatMessageResponse>> sendMessage(
            @RequestParam UUID requestId,
            @RequestParam UUID artisanId,
            @RequestBody @Valid SendMessageRequest request,
            Authentication authentication) {
        
        UUID senderId = UUID.fromString(authentication.getName());
        request.setRequestId(requestId);
        
        ChatMessageResponse response = chatService.sendPrivateMessage(request, senderId, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Gửi tin nhắn thành công", response));
    }

    // ============= REST API - Get Messages =============
    
    /**
     * Get all messages in a conversation between customer and artisan.
     */
    @GetMapping("/api/messages")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<List<ChatMessageResponse>>> getConversationMessages(
            @RequestParam UUID requestId,
            @RequestParam UUID artisanId,
            Authentication authentication) {
        
        List<ChatMessageResponse> messages = chatService.getPrivateMessages(requestId, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy tin nhắn thành công", messages));
    }
    
    /**
     * Get all messages for a request (across all conversations).
     * Useful for customer to see all chats with different artisans.
     */
    @GetMapping("/api/messages/request/{requestId}")
    @ResponseBody
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<List<ChatMessageResponse>>> getAllRequestMessages(
            @PathVariable UUID requestId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        List<ChatMessageResponse> messages = chatService.getChatHistory(requestId, userId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy tin nhắn thành công", messages));
    }

    // ============= Utility Endpoints =============
    
    /**
     * Mark messages as read for a request.
     */
    @PostMapping("/api/messages/mark-read")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<Void>> markMessagesAsRead(
            @RequestParam UUID requestId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        chatService.markMessagesAsRead(requestId, userId);
        
        return ResponseEntity.ok(BaseResponse.success("Đánh dấu đã đọc thành công"));
    }
}
