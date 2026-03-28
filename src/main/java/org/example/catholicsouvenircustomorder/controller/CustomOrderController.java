package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateOrderWithStagesDTO;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.model.CustomOrderStatus;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;
import org.example.catholicsouvenircustomorder.service.CustomOrderService;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for CustomOrder operations.
 * Handles custom order queries with role-based access control.
 * Requirements: 8.1, 8.2
 */
@Slf4j
@RestController
@RequestMapping("/api/custom-orders")
@RequiredArgsConstructor
public class CustomOrderController {
    
    private final CustomOrderService customOrderService;
    private final PaymentService paymentService;
    
    // ==================== COMMON ENDPOINTS ====================
    
    /**
     * Get custom orders (role-based: customer sees own, artisan sees own)
     * GET /api/custom-orders?status={status}&page={page}&size={size}
     * Requirements: 8.1, 8.2
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getCustomOrders(
            @RequestParam(required = false) CustomOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        
        log.info("User {} with role {} fetching custom orders with status {}", userId, role, status);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomOrderResponse> response;
        
        if ("CUSTOMER".equals(role)) {
            if (status != null) {
                response = customOrderService.getCustomerOrders(userId, status, pageable);
            } else {
                response = customOrderService.getCustomerOrders(userId, pageable);
            }
        } else if ("ARTISAN".equals(role)) {
            if (status != null) {
                response = customOrderService.getArtisanOrders(userId, status, pageable);
            } else {
                response = customOrderService.getArtisanOrders(userId, pageable);
            }
        } else {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Không có quyền truy cập"));
        }
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách đơn hàng thành công", response));
    }
    
    /**
     * Get custom order detail (ownership check)
     * GET /api/custom-orders/{id}
     * Requirements: 8.1, 8.2
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getCustomOrderDetail(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.info("User {} fetching custom order detail {}", userId, id);
        
        CustomOrderDetailResponse response = customOrderService.getOrderDetail(id);
        
        // Verify ownership - user must be either the customer or the artisan
        boolean isCustomer = response.getCustomerId().equals(userId);
        boolean isArtisan = response.getArtisanId().equals(userId);
        
        if (!isCustomer && !isArtisan) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Bạn không có quyền xem đơn hàng này"));
        }
        
        return ResponseEntity.ok(BaseResponse.success("Lấy chi tiết đơn hàng thành công", response));
    }
    
    // ==================== ARTISAN ENDPOINTS ====================
    
    /**
     * Update custom order status (Artisan only)
     * PUT /api/custom-orders/{id}/status
     * Requirements: 8.3, 8.4
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam CustomOrderStatus status,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("Artisan {} updating order {} status to {}", artisanId, id, status);
        
        CustomOrderResponse response = customOrderService.updateStatus(id, status, artisanId);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật trạng thái đơn hàng thành công", response));
    }
    
    /**
     * Create order from negotiation with stages (Artisan only, Request-Based flow)
     * POST /api/custom-orders/from-request/{requestId}
     * Requirements: RB-3
     */
    @PostMapping("/from-request/{requestId}")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> createOrderFromNegotiation(
            @PathVariable UUID requestId,
            @Valid @RequestBody CreateOrderWithStagesDTO request,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("Artisan {} creating order from negotiation for request {} with {} stages", 
            artisanId, requestId, request.getStages().size());
        
        CustomOrderResponse response = customOrderService.createFromNegotiation(requestId, artisanId, request);
        return ResponseEntity.ok(BaseResponse.success("Tạo đơn hàng thành công", response));
    }
    
    /**
     * Cancel custom order (Customer or Artisan)
     * POST /api/custom-orders/{id}/cancel
     * Requirements: 8.5
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> cancelOrder(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.info("User {} canceling order {} with reason: {}", userId, id, reason);
        
        CustomOrderResponse response = customOrderService.cancelOrder(id, userId, reason);
        return ResponseEntity.ok(BaseResponse.success("Hủy đơn hàng thành công", response));
    }
    
    /**
     * Initiate payment for custom order (Customer only)
     * POST /api/custom-orders/{id}/payment/initiate
     * Requirements: Payment flow for template-based custom orders
     */
    @PostMapping("/{id}/payment/initiate")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> initiatePayment(
            @PathVariable UUID id,
            @RequestParam PaymentMethod method,
            @RequestParam(required = false) String returnUrl,
            @RequestParam(required = false) String cancelUrl,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} initiating payment for custom order {} with method {}", customerId, id, method);
        
        // Verify order exists and belongs to customer
        CustomOrderDetailResponse orderDetail = customOrderService.getOrderDetail(id);
        if (!orderDetail.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Bạn không có quyền thanh toán đơn hàng này"));
        }
        
        // Check if order is in PENDING_PAYMENT status
        if (orderDetail.getStatus() != CustomOrderStatus.PENDING_PAYMENT) {
            return ResponseEntity.status(400)
                    .body(BaseResponse.error(400, "Đơn hàng không ở trạng thái chờ thanh toán"));
        }
        
        // Create payment initiation DTO
        InitiatePaymentDTO paymentDTO = InitiatePaymentDTO.builder()
                .customOrderId(id)
                .method(method)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();
        
        PaymentInitiationResponse response = paymentService.initiatePayment(paymentDTO);
        return ResponseEntity.ok(BaseResponse.success("Khởi tạo thanh toán thành công", response));
    }
}
