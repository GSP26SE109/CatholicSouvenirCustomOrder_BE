package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.NotificationResponse;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<BaseResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        
        Page<NotificationResponse> notifications = notificationService
                .getUserNotifications(userId, pageable);
        
        return ResponseEntity.ok(BaseResponse.<Page<NotificationResponse>>builder()
                .code(200)
                .message("Lấy danh sách thông báo thành công")
                .data(notifications)
                .build());
    }
    
    @GetMapping("/unread")
    public ResponseEntity<BaseResponse<Page<NotificationResponse>>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        
        Page<NotificationResponse> notifications = notificationService
                .getUnreadNotifications(userId, pageable);
        
        return ResponseEntity.ok(BaseResponse.<Page<NotificationResponse>>builder()
                .code(200)
                .message("Lấy danh sách thông báo chưa đọc thành công")
                .data(notifications)
                .build());
    }
    
    @GetMapping("/actionable")
    public ResponseEntity<BaseResponse<List<NotificationResponse>>> getActionableNotifications(
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        
        List<NotificationResponse> notifications = notificationService
                .getActionableNotifications(userId);
        
        return ResponseEntity.ok(BaseResponse.<List<NotificationResponse>>builder()
                .code(200)
                .message("Lấy danh sách thông báo cần hành động thành công")
                .data(notifications)
                .build());
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<BaseResponse<Long>> getUnreadCount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Long count = notificationService.getUnreadCount(userId);
        
        return ResponseEntity.ok(BaseResponse.<Long>builder()
                .code(200)
                .message("Lấy số lượng thông báo chưa đọc thành công")
                .data(count)
                .build());
    }
    
    @GetMapping("/actionable-count")
    public ResponseEntity<BaseResponse<Long>> getActionableCount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Long count = notificationService.getActionableCount(userId);
        
        return ResponseEntity.ok(BaseResponse.<Long>builder()
                .code(200)
                .message("Lấy số lượng thông báo cần hành động thành công")
                .data(count)
                .build());
    }
    
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<BaseResponse<Void>> markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication) {
        
        notificationService.markAsRead(notificationId);
        
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .code(200)
                .message("Đã đánh dấu thông báo là đã đọc")
                .build());
    }
    
    @PostMapping("/read-all")
    public ResponseEntity<BaseResponse<Void>> markAllAsRead(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.markAllAsRead(userId);
        
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .code(200)
                .message("Đã đánh dấu tất cả thông báo là đã đọc")
                .build());
    }
    
    @PostMapping("/{notificationId}/accept-request")
    public ResponseEntity<BaseResponse<Void>> acceptRequest(
            @PathVariable UUID notificationId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.acceptCustomRequest(notificationId, userId);
        
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .code(200)
                .message("Chấp nhận yêu cầu thành công")
                .build());
    }
    
    @PostMapping("/{notificationId}/reject-request")
    public ResponseEntity<BaseResponse<Void>> rejectRequest(
            @PathVariable UUID notificationId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        String reason = body.getOrDefault("reason", "No reason provided");
        
        notificationService.rejectCustomRequest(notificationId, userId, reason);
        
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .code(200)
                .message("Từ chối yêu cầu thành công")
                .build());
    }
    
    @PostMapping("/{notificationId}/complete-action")
    public ResponseEntity<BaseResponse<Void>> completeAction(
            @PathVariable UUID notificationId,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.completeNotificationAction(notificationId, userId);
        
        return ResponseEntity.ok(BaseResponse.<Void>builder()
                .code(200)
                .message("Hoàn thành hành động thành công")
                .build());
    }
}
