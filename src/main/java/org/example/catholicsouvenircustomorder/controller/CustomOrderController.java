package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateCustomOrderRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;
import org.example.catholicsouvenircustomorder.service.CustomOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/custom-orders")
@RequiredArgsConstructor
public class CustomOrderController {

    private final CustomOrderService customOrderService;

    @PostMapping
    public ResponseEntity<BaseResponse<CustomOrderResponse>> createCustomOrder(
            @Valid @RequestBody CreateCustomOrderRequest request,
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        CustomOrderResponse response = customOrderService.createCustomOrder(request, artisanId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Tạo đơn hàng tùy chỉnh thành công", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<BaseResponse<CustomOrderResponse>> getCustomOrder(
            @PathVariable UUID orderId) {
        
        CustomOrderResponse response = customOrderService.getCustomOrderById(orderId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy đơn hàng tùy chỉnh thành công", response));
    }

    @GetMapping("/customer/my-orders")
    public ResponseEntity<BaseResponse<List<CustomOrderResponse>>> getMyOrders(
            Authentication authentication) {
        
        UUID customerId = UUID.fromString(authentication.getName());
        List<CustomOrderResponse> responses = customOrderService.getCustomerOrders(customerId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách đơn hàng thành công", responses));
    }

    @GetMapping("/artisan/my-orders")
    public ResponseEntity<BaseResponse<List<CustomOrderResponse>>> getArtisanOrders(
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        List<CustomOrderResponse> responses = customOrderService.getArtisanOrders(artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách đơn hàng thành công", responses));
    }

    @PostMapping("/stages/complete")
    public ResponseEntity<BaseResponse<CustomOrderResponse>> completeStage(
            @Valid @RequestBody CompleteStageRequest request,
            Authentication authentication) {
        
        UUID artisanId = UUID.fromString(authentication.getName());
        CustomOrderResponse response = customOrderService.completeStage(request, artisanId);
        
        return ResponseEntity.ok(BaseResponse.success("Hoàn thành giai đoạn thành công", response));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<BaseResponse<CustomOrderResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam String status) {
        
        CustomOrderResponse response = customOrderService.updateOrderStatus(orderId, status);
        
        return ResponseEntity.ok(BaseResponse.success("Cập nhật trạng thái đơn hàng thành công", response));
    }
}
