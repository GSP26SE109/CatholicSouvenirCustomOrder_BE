package org.example.catholicsouvenircustomorder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CancelOrderRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateOrderWithStagesDTO;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.response.CancelOrderResponse;
import org.example.catholicsouvenircustomorder.dto.response.CancellationEstimate;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.StageRefundCalculation;

import java.util.ArrayList;
import java.util.List;
import org.example.catholicsouvenircustomorder.exception.InsufficientBalanceException;
import org.example.catholicsouvenircustomorder.model.CancellationInitiator;
import org.example.catholicsouvenircustomorder.model.CustomOrderStatus;
import org.example.catholicsouvenircustomorder.model.PaymentMethod;
import org.example.catholicsouvenircustomorder.model.RefundTransaction;
import org.example.catholicsouvenircustomorder.service.CancellationService;
import org.example.catholicsouvenircustomorder.service.CustomOrderService;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.example.catholicsouvenircustomorder.service.RefundCalculationService;
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
    private final RefundCalculationService refundCalculationService;
    private final CancellationService cancellationService;
    private final ObjectMapper objectMapper;
    
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
    
    /**
     * Get custom order by request ID (for customer to view order created by artisan)
     * GET /api/custom-orders/by-request/{requestId}
     * Requirements: RB-3 (Customer needs to view order before confirming)
     */
    @GetMapping("/by-request/{requestId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getCustomOrderByRequestId(
            @PathVariable UUID requestId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.info("User {} fetching custom order for request {}", userId, requestId);
        
        CustomOrderResponse response = customOrderService.getOrderByRequestId(requestId);
        
        // Verify ownership - user must be either the customer or the artisan
        boolean isCustomer = response.getCustomerId().equals(userId);
        boolean isArtisan = response.getArtisanId().equals(userId);
        
        if (!isCustomer && !isArtisan) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Bạn không có quyền xem đơn hàng này"));
        }
        
        return ResponseEntity.ok(BaseResponse.success("Lấy đơn hàng thành công", response));
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
     * POST /api/custom-orders
     * Requirements: RB-3
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> createOrder(
            @Valid @RequestBody CreateOrderWithStagesDTO request,
            Authentication authentication) {
        UUID artisanId = (UUID) authentication.getPrincipal();
        log.info("Artisan {} creating order from negotiation for request {} with {} stages", 
            artisanId, request.getRequestId(), request.getStages().size());
        
        CustomOrderResponse response = customOrderService.createFromNegotiation(request.getRequestId(), artisanId, request);
        return ResponseEntity.ok(BaseResponse.success("Tạo đơn hàng thành công", response));
    }
    
    /**
     * Cancel custom order (Customer or Artisan)
     * POST /api/custom-orders/{orderId}/cancel
     * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        
        log.info("User {} ({}) canceling order {} with reason: {}", userId, role, orderId, request.getReason());
        
        // Verify ownership first
        CustomOrderDetailResponse order = customOrderService.getOrderDetail(orderId);
        boolean isCustomer = order.getCustomerId().equals(userId);
        boolean isArtisan = order.getArtisanId().equals(userId);
        
        if (!isCustomer && !isArtisan) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Bạn không có quyền hủy đơn hàng này"));
        }
        
        // Determine initiator based on role
        CancellationInitiator initiator = "CUSTOMER".equals(role) 
            ? CancellationInitiator.CUSTOMER 
            : CancellationInitiator.ARTISAN;
        
        try {
            // Call CancellationService.cancelOrder()
            RefundTransaction refundTransaction = cancellationService.cancelOrder(
                orderId,
                userId,
                initiator,
                request.getReason()
            );
            
            // Parse calculation details JSON to get stage breakdown
            List<StageRefundCalculation> stageBreakdown = parseCalculationDetails(
                refundTransaction.getCalculationDetails()
            );
            
            // Build response
            CancelOrderResponse response = new CancelOrderResponse(
                refundTransaction.getRefundTransactionId(),
                refundTransaction.getAmount(),
                refundTransaction.getPlatformCommissionAmount(),
                refundTransaction.getNetRefundAmount(),
                stageBreakdown,
                refundTransaction.getStatus().name()
            );
            
            return ResponseEntity.ok(BaseResponse.success("Hủy đơn hàng thành công", response));
            
        } catch (InsufficientBalanceException e) {
            log.error("Insufficient balance for cancellation: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(BaseResponse.error(400, e.getMessage()));
        }
    }
    
    /**
     * Parse calculation details JSON string to StageRefundCalculation list
     */
    private List<StageRefundCalculation> parseCalculationDetails(String calculationDetailsJson) {
        try {
            if (calculationDetailsJson == null || calculationDetailsJson.isEmpty()) {
                return new ArrayList<>();
            }
            
            return objectMapper.readValue(
                calculationDetailsJson,
                objectMapper.getTypeFactory().constructCollectionType(
                    List.class, 
                    StageRefundCalculation.class
                )
            );
        } catch (Exception e) {
            log.error("Failed to parse calculation details: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    /**
     * Get stages of a custom order
     * GET /api/custom-orders/{orderId}/stages
     * Requirements: RB-4 (Request-Based flow)
     */
    @GetMapping("/{orderId}/stages")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getOrderStages(
            @PathVariable UUID orderId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        log.info("User {} fetching stages for order {}", userId, orderId);
        
        // Verify ownership first
        CustomOrderDetailResponse order = customOrderService.getOrderDetail(orderId);
        boolean isCustomer = order.getCustomerId().equals(userId);
        boolean isArtisan = order.getArtisanId().equals(userId);
        
        if (!isCustomer && !isArtisan) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Bạn không có quyền xem các giai đoạn của đơn hàng này"));
        }
        
        // Get stages with payment status
        List<CustomOrderStageResponse> stages = customOrderService.getOrderStages(orderId);
        
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách giai đoạn thành công", stages));
    }
    
    /**
     * Customer confirms custom order (Request-Based flow)
     * POST /api/custom-orders/{orderId}/confirm
     * Requirements: RB-3 (Customer must confirm before payment)
     */
    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> confirmOrder(
            @PathVariable UUID orderId,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} confirming order {}", customerId, orderId);
        
        CustomOrderResponse response = customOrderService.confirmOrder(orderId, customerId);
        return ResponseEntity.ok(BaseResponse.success("Xác nhận đơn hàng thành công. Bạn có thể bắt đầu thanh toán giai đoạn đầu tiên.", response));
    }
    
    /**
     * Customer rejects custom order (Request-Based flow)
     * POST /api/custom-orders/{orderId}/reject
     * Requirements: RB-3 (Customer can reject order before payment)
     * Only allowed when order is in PENDING_CONFIRMATION status
     * No refund needed since no payment has been made
     */
    @PostMapping("/{orderId}/reject")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> rejectOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody org.example.catholicsouvenircustomorder.dto.request.RejectOrderRequest request,
            Authentication authentication) {
        UUID customerId = (UUID) authentication.getPrincipal();
        log.info("Customer {} rejecting order {} with reason: {}", customerId, orderId, request.getReason());
        
        CustomOrderResponse response = customOrderService.rejectOrder(orderId, customerId, request.getReason());
        return ResponseEntity.ok(BaseResponse.success("Từ chối đơn hàng thành công", response));
    }
    
    /**
     * Get refund estimate for order cancellation
     * GET /api/custom-orders/{orderId}/refund-estimate
     * Requirements: 7.5
     */
    @GetMapping("/{orderId}/refund-estimate")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getRefundEstimate(
            @PathVariable UUID orderId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        
        log.info("User {} ({}) requesting refund estimate for order {}", userId, role, orderId);
        
        // Verify ownership first
        CustomOrderDetailResponse order = customOrderService.getOrderDetail(orderId);
        boolean isCustomer = order.getCustomerId().equals(userId);
        boolean isArtisan = order.getArtisanId().equals(userId);
        
        if (!isCustomer && !isArtisan) {
            return ResponseEntity.status(403)
                    .body(BaseResponse.error(403, "Bạn không có quyền xem ước tính hoàn tiền cho đơn hàng này"));
        }
        
        // Determine initiator based on role
        CancellationInitiator initiator = "CUSTOMER".equals(role) 
            ? CancellationInitiator.CUSTOMER 
            : CancellationInitiator.ARTISAN;
        
        CancellationEstimate estimate = refundCalculationService.calculateRefundEstimate(orderId, initiator);
        
        return ResponseEntity.ok(BaseResponse.success("Tính toán ước tính hoàn tiền thành công", estimate));
    }
    
    /**
     * NOTE: CustomOrder uses STAGE-BASED payment flow
     * This endpoint is REMOVED - use /api/stage-payments/{stageId}/initiate instead
     * CustomOrder payments are handled per stage, not for the entire order
     */
}
