package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.SendMessageRequest;
import org.example.catholicsouvenircustomorder.dto.response.ChatMessageResponse;
import org.example.catholicsouvenircustomorder.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/{requestId}/{artisanId}")
    public void sendMessage(@Payload SendMessageRequest request, 
                          @DestinationVariable UUID requestId,
                          @DestinationVariable UUID artisanId,
                          Principal principal) {
        UUID senderId = null;
        if (principal != null && principal.getName() != null) {
            senderId = UUID.fromString(principal.getName());
        }
        
        request.setRequestId(requestId);
        chatService.sendPrivateMessage(request, senderId, artisanId);
    }

    // REST API - Private 1-1 Chat (Recommended)
    @PostMapping("/api/chat/{requestId}/{artisanId}/messages")
    @ResponseBody
    public ResponseEntity<BaseResponse<ChatMessageResponse>> sendPrivateMessageRest(
            @PathVariable UUID requestId,
            @PathVariable UUID artisanId,
            @RequestBody SendMessageRequest request,
            Authentication authentication) {
        
        UUID senderId = UUID.fromString(authentication.getName());
        request.setRequestId(requestId);
        
        ChatMessageResponse response = chatService.sendPrivateMessage(request, senderId, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Gửi tin nhắn riêng thành công", response));
    }

    // REST API - Legacy (Group chat)
    @PostMapping("/api/chat/messages")
    @ResponseBody
    public ResponseEntity<BaseResponse<ChatMessageResponse>> sendMessageRest(
            @RequestBody SendMessageRequest request,
            Authentication authentication) {
        
        UUID senderId = UUID.fromString(authentication.getName());
        ChatMessageResponse response = chatService.sendMessage(request, senderId);
        
        return ResponseEntity.ok(BaseResponse.success("Gửi tin nhắn thành công", response));
    }

    @GetMapping("/api/chat/{requestId}/messages")
    @ResponseBody
    public ResponseEntity<BaseResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable UUID requestId) {
        
        List<ChatMessageResponse> messages = chatService.getMessagesByRequest(requestId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy tin nhắn thành công", messages));
    }

    @GetMapping("/api/chat/{requestId}/{artisanId}/messages")
    @ResponseBody
    public ResponseEntity<BaseResponse<List<ChatMessageResponse>>> getPrivateMessages(
            @PathVariable UUID requestId,
            @PathVariable UUID artisanId) {
        
        List<ChatMessageResponse> messages = chatService.getPrivateMessages(requestId, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy tin nhắn riêng thành công", messages));

    }

    @PostMapping("/api/chat/{requestId}/mark-read")
    @ResponseBody
    public ResponseEntity<BaseResponse<Void>> markMessagesAsRead(
            @PathVariable UUID requestId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        chatService.markMessagesAsRead(requestId, userId);
        
        return ResponseEntity.ok(BaseResponse.success("Đánh dấu đã đọc thành công"));
    }
}
