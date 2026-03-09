package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.ConfirmArtisanRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateCustomRequestRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;
import org.example.catholicsouvenircustomorder.service.CustomRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/custom-requests")
@RequiredArgsConstructor
public class CustomRequestController {

    private final CustomRequestService customRequestService;

    @PostMapping
    public ResponseEntity<BaseResponse<CustomRequestResponse>> createCustomRequest(
            @Valid @RequestBody CreateCustomRequestRequest request,
            @AuthenticationPrincipal UUID customerId) {

        CustomRequestResponse response = customRequestService.createCustomRequest(request, customerId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Tạo yêu cầu tùy chỉnh thành công", response));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<BaseResponse<CustomRequestResponse>> getCustomRequest(
            @PathVariable UUID requestId) {
        
        CustomRequestResponse response = customRequestService.getCustomRequestById(requestId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy yêu cầu tùy chỉnh thành công", response));
    }

    @GetMapping("/customer/my-requests")
    public ResponseEntity<BaseResponse<List<CustomRequestResponse>>> getMyRequests(
            Authentication authentication) {
        
        UUID customerId = UUID.fromString(authentication.getName());
        List<CustomRequestResponse> responses = customRequestService.getCustomerRequests(customerId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách yêu cầu thành công", responses));
    }

    @GetMapping("/artisan/my-requests")
    public ResponseEntity<BaseResponse<List<CustomRequestResponse>>> getArtisanRequests(
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        List<CustomRequestResponse> responses = customRequestService.getArtisanRequests(artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách yêu cầu thành công", responses));
    }

    @PostMapping("/confirm-artisan")
    public ResponseEntity<BaseResponse<CustomRequestResponse>> confirmArtisan(
            @Valid @RequestBody ConfirmArtisanRequest request,
            Authentication authentication) {
        
        UUID customerId = UUID.fromString(authentication.getName());
        CustomRequestResponse response = customRequestService.confirmArtisan(request, customerId);
        
        return ResponseEntity.ok(BaseResponse.success("Xác nhận thợ thủ công thành công", response));
    }
}
