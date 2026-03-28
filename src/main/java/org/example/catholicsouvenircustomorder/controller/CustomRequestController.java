package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateCustomRequestRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateFreeFormRequestDTO;
import org.example.catholicsouvenircustomorder.dto.request.RejectCustomRequestRequest;
import org.example.catholicsouvenircustomorder.dto.request.SelectArtisanRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;
import org.example.catholicsouvenircustomorder.service.CustomRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/custom-requests")
@RequiredArgsConstructor
public class CustomRequestController {
    
    private final CustomRequestService customRequestService;
    
    @PostMapping("/template")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> createTemplateBasedRequest(
            @Valid @RequestBody CreateCustomRequestRequest request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.createFromTemplate(request, customerId);
        return ResponseEntity.ok(BaseResponse.success("Tạo yêu cầu tùy chỉnh thành công", response));
    }
    
    @PostMapping("/free-form")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> createFreeFormRequest(
            @Valid @RequestBody CreateFreeFormRequestDTO request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.createFreeFormRequest(request, customerId);
        return ResponseEntity.ok(BaseResponse.success("Tạo yêu cầu tự do thành công", response));
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> getCustomerRequests(
            @RequestParam(required = false) CustomRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomRequestResponse> response = customRequestService.getCustomerRequests(customerId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách yêu cầu thành công", response));
    }
    
    @GetMapping("/open")
    public ResponseEntity<BaseResponse> getOpenRequests(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomRequestResponse> response = customRequestService.getOpenRequests(category, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách yêu cầu mở thành công", response));
    }
    
    @PostMapping("/{id}/select-artisan")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> selectArtisan(
            @PathVariable UUID id,
            @Valid @RequestBody SelectArtisanRequest request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.selectArtisan(id, request.getArtisanId(), customerId);
        return ResponseEntity.ok(BaseResponse.success("Chọn nghệ nhân thành công", response));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> getCustomRequestDetail(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.getRequestDetail(id);
        
        if (!response.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Bạn không có quyền xem yêu cầu này"));
        }
        
        return ResponseEntity.ok(BaseResponse.success("Lấy chi tiết yêu cầu thành công", response));
    }
    
    @PostMapping("/{id}/regenerate-image")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> regenerateAIImage(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.regenerateAIImage(id, customerId);
        return ResponseEntity.ok(BaseResponse.success("Tạo lại ảnh AI thành công", response));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> cancelCustomRequest(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        CustomRequestResponse request = customRequestService.getRequestDetail(id);
        
        if (!request.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Bạn không có quyền hủy yêu cầu này"));
        }
        
        if (request.getStatus() != CustomRequestStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error(400, "Chỉ có thể hủy yêu cầu đang chờ xử lý"));
        }
        
        return ResponseEntity.ok(BaseResponse.success("Hủy yêu cầu thành công"));
    }
    
    @GetMapping("/artisan")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> getArtisanCustomRequests(
            @RequestParam(required = false) CustomRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomRequestResponse> response = customRequestService.getArtisanRequests(artisanId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách yêu cầu thành công", response));
    }
    
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> acceptCustomRequest(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.acceptRequest(id, artisanId);
        return ResponseEntity.ok(BaseResponse.success("Chấp nhận yêu cầu thành công", response));
    }
    
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> rejectCustomRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectCustomRequestRequest request,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        CustomRequestResponse response = customRequestService.rejectRequest(id, artisanId, request.getReason());
        return ResponseEntity.ok(BaseResponse.success("Từ chối yêu cầu thành công", response));
    }
}
