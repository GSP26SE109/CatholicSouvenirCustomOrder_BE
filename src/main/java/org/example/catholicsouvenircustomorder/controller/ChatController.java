package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;
import org.example.catholicsouvenircustomorder.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
 * Supports conversation-based chat with WebSocket real-time messaging.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ============= WebSocket Endpoints =============
    
    /**
     * WebSocket endpoint for sending messages in conversation.
     * Client subscribes to: /topic/chat/{conversationId}
     * Client sends to: /app/chat/{conversationId}
     */
    @MessageMapping("/chat/{conversationId}")
    public void sendMessageViaWebSocket(
            @Payload SendMessageRequest request, 
            @DestinationVariable UUID conversationId,
            Principal principal) {
        
        UUID senderId = UUID.fromString(principal.getName());
        request.setConversationId(conversationId);
        chatService.sendMessage(request, senderId);
    }

    // ============= REST API - Send Messages =============
    
    /**
     * Send message via REST API.
     * POST /api/chat/send
     */
    @PostMapping("/api/chat/send")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<ChatMessageResponse>> sendMessage(
            @RequestBody @Valid SendMessageRequest request,
            Authentication authentication) {
        
        UUID senderId = UUID.fromString(authentication.getName());
        ChatMessageResponse response = chatService.sendMessage(request, senderId);
        
        return ResponseEntity.ok(BaseResponse.success("Gửi tin nhắn thành công", response));
    }

    // ============= REST API - Get Messages =============
    
    /**
     * Get messages in a conversation with pagination.
     * GET /api/chat/conversation/{conversationId}/messages
     */
    @GetMapping("/api/chat/conversation/{conversationId}/messages")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<Page<ChatMessageResponse>>> getConversationMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessageResponse> messages = chatService.getConversationMessages(conversationId, userId, pageable);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy tin nhắn thành công", messages));
    }
    
    /**
     * Get all conversations for a user.
     * GET /api/chat/conversations
     */
    @GetMapping("/api/chat/conversations")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<List<ChatMessageResponse>>> getUserConversations(
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        List<ChatMessageResponse> conversations = chatService.getUserConversations(userId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy cuộc trò chuyện thành công", conversations));
    }

    // ============= Utility Endpoints =============
    
    /**
     * Mark messages as read in a conversation.
     * POST /api/chat/conversation/{conversationId}/mark-read
     */
    @PostMapping("/api/chat/conversation/{conversationId}/mark-read")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<Void>> markMessagesAsRead(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        chatService.markMessagesAsRead(conversationId, userId);
        
        return ResponseEntity.ok(BaseResponse.success("Đánh dấu đã đọc thành công"));
    }
    
    /**
     * Get unread message count for user.
     * GET /api/chat/unread-count
     */
    @GetMapping("/api/chat/unread-count")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<Long>> getUnreadMessageCount(
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        Long count = chatService.getUnreadMessageCount(userId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy số tin nhắn chưa đọc thành công", count));
    }
    
    /**
     * Get unread message count for specific conversation.
     * GET /api/chat/conversation/{conversationId}/unread-count
     */
    @GetMapping("/api/chat/conversation/{conversationId}/unread-count")
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse<Long>> getConversationUnreadCount(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        Long count = chatService.getUnreadMessageCount(conversationId, userId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy số tin nhắn chưa đọc thành công", count));
    }
}
