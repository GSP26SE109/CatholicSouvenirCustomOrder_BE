package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.PaymentCallbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.model.CustomOrder;
import org.example.catholicsouvenircustomorder.model.CustomOrderStage;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.repository.CustomOrderRepository;
import org.example.catholicsouvenircustomorder.repository.CustomOrderStageRepository;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.PaymentRepository;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final CustomOrderStageRepository stageRepository;
    private final PaymentRepository paymentRepository;
    
    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse<PaymentInitiationResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentDTO dto,
            @AuthenticationPrincipal UUID accountId) {
        
        // Verify ownership

        if (dto.getOrderId() != null) {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            if (!order.getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền thanh toán đơn hàng này");
            }
        } else if (dto.getCustomOrderId() != null) {
            CustomOrder customOrder = customOrderRepository.findById(dto.getCustomOrderId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng tùy chỉnh"));
            if (!customOrder.getRequest().getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền thanh toán đơn hàng này");
            }
        } else if (dto.getStageId() != null) {
            CustomOrderStage stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy giai đoạn"));
            if (!stage.getCustomOrder().getRequest().getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền thanh toán giai đoạn này");
            }
        }
        
        PaymentInitiationResponse response = paymentService.initiatePayment(dto);
        
        return ResponseEntity.ok(BaseResponse.<PaymentInitiationResponse>builder()
                .code(200)
                .message("Khởi tạo thanh toán thành công")
                .data(response)
                .build());
    }
    
    @PostMapping("/callback")
    public ResponseEntity<BaseResponse<PaymentResponse>> handlePaymentCallback(
            @RequestBody Map<String, String> params,
            @RequestParam(required = false, defaultValue = "VNPAY") String gateway) {
        
        PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                .params(params)
                .paymentGateway(gateway)
                .build();
        
        PaymentResponse response = paymentService.handlePaymentCallback(request);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Xử lý callback thanh toán thành công")
                .data(response)
                .build());
    }
    
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<PaymentResponse>>> getUserPayments(
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        List<PaymentResponse> payments = new java.util.ArrayList<>();
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        if ("CUSTOMER".equals(role)) {
            // Get all payments for customer's orders and custom orders
            List<Order> orders = orderRepository.findByCustomer_AccountId(accountId);
            for (Order order : orders) {
                payments.addAll(paymentService.getOrderPayments(order.getOrderId()));
            }
            
            List<CustomOrder> customOrders = customOrderRepository.findByRequest_Customer_AccountId(accountId);
            for (CustomOrder customOrder : customOrders) {
                payments.addAll(paymentService.getCustomOrderPayments(customOrder.getCustomOrderId()));
            }
            
        } else if ("ARTISAN".equals(role)) {
            // Get all payments for artisan's custom orders
            List<CustomOrder> customOrders = customOrderRepository.findByArtisan_ArtisanUuid(accountId);
            for (CustomOrder customOrder : customOrders) {
                payments.addAll(paymentService.getCustomOrderPayments(customOrder.getCustomOrderId()));
            }
            
        } else if ("ADMIN".equals(role)) {
            // Admin can see all payments
            payments = paymentRepository.findAll().stream()
                    .map(payment -> {
                        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                                .paymentId(payment.getPaymentId())
                                .paymentMethod(payment.getMethod())
                                .amount(payment.getAmount())
                                .status(payment.getStatus())
                                .transactionId(payment.getTransactionId())
                                .paymentUrl(payment.getPaymentUrl())
                                .failureReason(payment.getFailureReason())
                                .createdAt(payment.getCreatedAt())
                                .paidAt(payment.getPaidAt());
                        
                        if (payment.getOrder() != null) {
                            builder.orderId(payment.getOrder().getOrderId());
                        }
                        if (payment.getCustomOrder() != null) {
                            builder.customOrderId(payment.getCustomOrder().getCustomOrderId());
                        }
                        if (payment.getStage() != null) {
                            builder.stageId(payment.getStage().getStageId())
                                   .stageName(payment.getStage().getName());
                        }
                        
                        return builder.build();
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
        
        return ResponseEntity.ok(BaseResponse.<List<PaymentResponse>>builder()
                .code(200)
                .message("Lấy danh sách thanh toán thành công")
                .data(payments)
                .build());
    }
    
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<PaymentResponse>>> getOrderPayments(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            if (!order.getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán của đơn hàng này");
            }
        }
        
        List<PaymentResponse> payments = paymentService.getOrderPayments(orderId);
        
        return ResponseEntity.ok(BaseResponse.<List<PaymentResponse>>builder()
                .code(200)
                .message("Lấy danh sách thanh toán thành công")
                .data(payments)
                .build());
    }
    
    @GetMapping("/custom-order/{customOrderId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<PaymentResponse>>> getCustomOrderPayments(
            @PathVariable UUID customOrderId,
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        // Verify ownership for customers and artisans
        CustomOrder customOrder = customOrderRepository.findById(customOrderId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng tùy chỉnh"));
        
        if ("CUSTOMER".equals(role)) {
            if (!customOrder.getRequest().getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán của đơn hàng này");
            }
        } else if ("ARTISAN".equals(role)) {
            if (!customOrder.getArtisan().getArtisanUuid().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán của đơn hàng này");
            }
        }
        
        List<PaymentResponse> payments = paymentService.getCustomOrderPayments(customOrderId);
        
        return ResponseEntity.ok(BaseResponse.<List<PaymentResponse>>builder()
                .code(200)
                .message("Lấy danh sách thanh toán thành công")
                .data(payments)
                .build());
    }
    
    @GetMapping("/stage/{stageId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<PaymentResponse>>> getStagePayments(
            @PathVariable UUID stageId,
            @AuthenticationPrincipal UUID accountId,
            Authentication authentication) {
        
        // Get role from authorities
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("")
                .replace("ROLE_", "");
        
        // Verify ownership for customers and artisans
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giai đoạn"));
        
        CustomOrder customOrder = stage.getCustomOrder();
        
        if ("CUSTOMER".equals(role)) {
            if (!customOrder.getRequest().getCustomer().getAccountId().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán của giai đoạn này");
            }
        } else if ("ARTISAN".equals(role)) {
            if (!customOrder.getArtisan().getArtisanUuid().equals(accountId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán của giai đoạn này");
            }
        }
        
        List<PaymentResponse> payments = paymentService.getStagePayments(stageId);
        
        return ResponseEntity.ok(BaseResponse.<List<PaymentResponse>>builder()
                .code(200)
                .message("Lấy danh sách thanh toán thành công")
                .data(payments)
                .build());
    }
    
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<PaymentResponse>> refundPayment(
            @PathVariable("id") UUID paymentId,
            @RequestParam String reason) {
        
        PaymentResponse response = paymentService.refundPayment(paymentId, reason);
        
        return ResponseEntity.ok(BaseResponse.<PaymentResponse>builder()
                .code(200)
                .message("Hoàn tiền thành công")
                .data(response)
                .build());
    }
}
