package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.PaymentCallbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.OrderGroupRepository;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.PaymentRepository;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.example.catholicsouvenircustomorder.service.SystemConfigService;
import org.example.catholicsouvenircustomorder.service.CommissionService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.InventoryService;
import org.example.catholicsouvenircustomorder.dto.CommissionCalculation;
import org.example.catholicsouvenircustomorder.util.VNPayUtil;
import org.example.catholicsouvenircustomorder.util.ZaloPayUtil;
import org.example.catholicsouvenircustomorder.config.VNPayConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for OrderGroup payments (checkout payments)
 * For custom order stage payments, use StagePaymentService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderGroupRepository orderGroupRepository;
    private final OrderRepository orderRepository;
    private final VNPayUtil vnPayUtil;
    private final VNPayConfig vnPayConfig;
    private final ZaloPayUtil zaloPayUtil;
    private final WalletServiceImp walletService;
    private final SystemConfigService systemConfigService;
    private final CommissionService commissionService;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(InitiatePaymentDTO dto, UUID customerId) {
        log.info("Initiating payment for order group: {} by customer: {}", dto.getOrderGroupId(), customerId);
        
        if (dto.getOrderGroupId() == null) {
            throw new BadRequestException("Order Group ID không được để trống");
        }
        
        OrderGroup orderGroup = orderGroupRepository.findById(dto.getOrderGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm đơn hàng"));
        
        // Validate ownership
        if (!orderGroup.getCustomer().getAccountId().equals(customerId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán nhóm đơn hàng này");
        }
        
        if (orderGroup.getOrders() == null || orderGroup.getOrders().isEmpty()) {
            throw new BadRequestException("Nhóm đơn hàng không có đơn hàng nào");
        }
        
        // CRITICAL: Check if order group is already paid
        if ("PAID".equalsIgnoreCase(orderGroup.getStatus())) {
            log.warn("⚠️ Order group {} is already PAID, cannot create new payment", orderGroup.getGroupId());
            throw new BadRequestException("Nhóm đơn hàng này đã được thanh toán");
        }
        
        // Check if already has successful payment
        boolean hasSuccessfulPayment = paymentRepository.isOrderGroupPaid(dto.getOrderGroupId());
        if (hasSuccessfulPayment) {
            log.warn("⚠️ Order group {} already has successful payment", orderGroup.getGroupId());
            throw new BadRequestException("Nhóm đơn hàng này đã được thanh toán");
        }
        
        // Check if already has pending payment
        Payment existingPayment = paymentRepository.findByOrderGroup_GroupIdAndStatus(
                dto.getOrderGroupId(), PaymentStatus.PENDING
        ).orElse(null);
        
        if (existingPayment != null) {
            // Check if existing payment is recent (within 15 minutes)
            LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
            if (existingPayment.getCreatedAt().isAfter(fifteenMinutesAgo)) {
                // Payment is recent, reuse it instead of creating new one
                log.info("⚠️ Found recent pending payment (created {}), reusing it instead of creating new one", 
                        existingPayment.getCreatedAt());
                
                return PaymentInitiationResponse.builder()
                        .paymentId(existingPayment.getPaymentId())
                        .paymentUrl(existingPayment.getPaymentUrl())
                        .transactionId(existingPayment.getReferenceId())
                        .amount(existingPayment.getAmount())
                        .build();
            } else {
                // Payment is old (>15 minutes), cancel it and create new one
                log.info("Found old pending payment: {} (created {}), cancelling it", 
                        existingPayment.getPaymentId(), existingPayment.getCreatedAt());
                existingPayment.setStatus(PaymentStatus.CANCELLED);
                existingPayment.setFailureReason("Replaced by new payment request (expired)");
                paymentRepository.save(existingPayment);
                log.info("Old payment cancelled, creating new one");
            }
        }
        
        // Create new payment
        Payment payment = new Payment();
        payment.setOrderGroup(orderGroup);
        payment.setMethod(dto.getMethod());
        payment.setAmount(orderGroup.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setReturnUrl(dto.getReturnUrl());
        
        // TỰ ĐỘNG lấy commission rate từ SystemConfigService và lưu vào Payment
        BigDecimal commissionRate = systemConfigService.getCommissionRate();
        payment.setCommissionRate(commissionRate);
        log.info("Payment created with commission_rate={}%", commissionRate);
        
        String referenceId = "GROUP_" + orderGroup.getGroupId() + "_" + System.currentTimeMillis();
        payment.setReferenceId(referenceId);
        
        String paymentUrl;
        try {
            int orderCount = orderGroup.getOrders().size();
            String description = orderCount == 1 
                ? "Thanh toan don hang" 
                : "Thanh toan " + orderCount + " don hang";
            
            if (dto.getMethod() == PaymentMethod.VNPAY) {
                // CRITICAL: vnp_ReturnUrl MUST point to BACKEND
                // Backend will update DB and then redirect to frontend
                // Save frontend URL to payment.returnUrl for later redirect
                String backendReturnUrl = baseUrl + "/api/payments/vnpay/return";
                
                log.info("Creating VNPay payment URL");
                log.info("Backend return URL (for VNPay): {}", backendReturnUrl);
                log.info("Frontend return URL (saved in DB): {}", dto.getReturnUrl());
                
                // IMPORTANT: Always use backend URL for VNPay, ignore frontend URL
                paymentUrl = vnPayUtil.createPaymentUrl(
                        referenceId,
                        orderGroup.getTotalAmount(),
                        description,
                        orderGroup.getCustomer().getEmail(),
                        backendReturnUrl  // Always use backend URL
                );
            } else if (dto.getMethod() == PaymentMethod.ZALOPAY) {
                throw new BadRequestException("ZaloPay không được hỗ trợ. Vui lòng sử dụng VNPay");
            } else {
                throw new BadRequestException("Phương thức thanh toán không được hỗ trợ");
            }
            
            payment.setPaymentUrl(paymentUrl);
            
        } catch (Exception e) {
            log.error("Error creating payment URL: ", e);
            throw new BadRequestException("Không thể tạo URL thanh toán: " + e.getMessage());
        }
        
        payment = paymentRepository.save(payment);
        log.info("Payment created successfully: {}", payment.getPaymentId());
        
        return PaymentInitiationResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentUrl(paymentUrl)
                .transactionId(referenceId)
                .amount(orderGroup.getTotalAmount())
                .build();
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public PaymentResponse handlePaymentCallback(PaymentCallbackRequest request) {
        log.info("========================================");
        log.info("Processing payment callback");
        log.info("Gateway: {}", request.getPaymentGateway());
        log.info("========================================");
        
        String referenceId;
        String gatewayTransactionId;
        String status;
        
        if ("VNPAY".equalsIgnoreCase(request.getPaymentGateway())) {
            // CRITICAL: Verify signature first
            log.info("Verifying VNPay signature...");
            log.info("Received params: {}", request.getParams());
            
            // VNPayUtil.verifySecureHash sẽ tự tạo copy, không cần pass copy nữa
            boolean isValidSignature = vnPayUtil.verifySecureHash(
                request.getParams(), 
                vnPayConfig.getHashSecret()
            );
            
            if (!isValidSignature) {
                log.error("VNPay signature verification FAILED!");
                log.error("Received hash: {}", request.getParams().get("vnp_SecureHash"));
                throw new BadRequestException("Chữ ký không hợp lệ");
            }
            
            log.info("VNPay signature verified successfully");
            
            referenceId = request.getParams().get("vnp_TxnRef");
            gatewayTransactionId = request.getParams().get("vnp_TransactionNo");
            status = request.getParams().get("vnp_ResponseCode");
            log.info("VNPay callback - TxnRef: {}, TransactionNo: {}, Status: {}", 
                    referenceId, gatewayTransactionId, status);
        } else if ("ZALOPAY".equalsIgnoreCase(request.getPaymentGateway())) {
            referenceId = request.getParams().get("apptransid");
            gatewayTransactionId = request.getParams().get("zptransid");
            status = request.getParams().get("status");
        } else {
            throw new BadRequestException("Payment gateway không được hỗ trợ");
        }
        
        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));
        
        // IDEMPOTENCY CHECK: If already processed, return existing result
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already processed successfully, returning existing result");
            return mapToPaymentResponse(payment);
        }
        
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Payment already marked as failed, returning existing result");
            return mapToPaymentResponse(payment);
        }
        
        if ("00".equals(status) || "SUCCESS".equalsIgnoreCase(status) || "1".equals(status)) {
            log.info("✅ Payment successful, updating status");
            
            // Validate order group exists FIRST
            OrderGroup orderGroup = payment.getOrderGroup();
            if (orderGroup == null) {
                log.error("❌ Payment {} has no associated order group!", payment.getPaymentId());
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("No order group associated with payment");
                payment = paymentRepository.save(payment);
                log.error("❌ Payment marked as FAILED and saved");
                return mapToPaymentResponse(payment);
            }
            
            log.info("📝 Setting payment status to SUCCESS");
            // Now we know orderGroup is not null, proceed with success flow
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(gatewayTransactionId);
            payment.setPaidAt(LocalDateTime.now());
            
            // CRITICAL: Save payment IMMEDIATELY to ensure status is persisted
            // This happens BEFORE distribution to prevent rollback issues
            log.info("💾 Saving payment with SUCCESS status BEFORE distribution");
            payment = paymentRepository.save(payment);
            log.info("✅ Payment saved successfully with status: {}", payment.getStatus());
            
            // Update order group status
            log.info("📝 Updating order group status to PAID");
            orderGroup.setStatus("PAID");
            orderGroup.setUpdatedAt(LocalDateTime.now());
            orderGroupRepository.save(orderGroup);
            log.info("✅ Order group {} status updated to PAID", orderGroup.getGroupId());
            
            // Update ALL orders in the group
            log.info("📝 Updating {} orders in group", orderGroup.getOrders().size());
            for (Order order : orderGroup.getOrders()) {
                order.setStatus("PAID");
                order.setUpdateAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("✅ Order {} status updated to PAID", order.getOrderId());
            }
            
            log.info("✅ Updated {} orders in group {}", 
                    orderGroup.getOrders().size(), orderGroup.getGroupId());
            
            // Confirm inventory reservations (convert reserved to actual deduction)
            log.info("📦 Confirming inventory reservations");
            try {
                inventoryService.confirmReservations(orderGroup);
                log.info("✅ Inventory reservations confirmed");
            } catch (Exception e) {
                log.error("❌ Error confirming inventory reservations: {}", e.getMessage(), e);
                // Continue with payment distribution even if inventory confirmation fails
            }
            
            // Distribute payment to all artisans with commission deduction
            log.info("💰 Starting payment distribution process");
            try {
                // Re-fetch payment with JOIN FETCH to avoid lazy loading issues
                log.info("🔄 Re-fetching payment with orders for distribution");
                Payment paymentForDistribution = paymentRepository
                        .findByIdWithOrdersForDistribution(payment.getPaymentId())
                        .orElse(payment);
                
                log.info("👤 Getting platform admin account");
                Account platformAdmin = walletService.getPlatformAdminAccount();
                
                // Calculate commission from the rate saved in Payment entity
                BigDecimal commissionRate = paymentForDistribution.getCommissionRate();
                if (commissionRate == null) {
                    commissionRate = BigDecimal.ZERO;
                    log.warn("⚠️ Payment {} has null commission rate, using 0", payment.getPaymentId());
                }
                
                log.info("🧮 Calculating commission with rate: {}%", commissionRate);
                CommissionCalculation commissionCalc = commissionService.calculateCommission(
                    paymentForDistribution.getAmount(), 
                    commissionRate
                );
                
                log.info("💵 Commission calculated for payment {}: original={}, commission={}, net={}", 
                    payment.getPaymentId(), 
                    commissionCalc.getOriginalAmount(),
                    commissionCalc.getCommissionAmount(),
                    commissionCalc.getNetAmount());
                
                // Process payment distribution with commission-adjusted amount
                log.info("🚀 Calling walletService.processPaymentDistribution");
                walletService.processPaymentDistribution(
                    paymentForDistribution, 
                    platformAdmin,
                    commissionCalc.getCommissionAmount(),
                    commissionRate
                );
                
                log.info("✅ Payment distribution completed for order group: {}", orderGroup.getGroupId());
            } catch (Exception e) {
                log.error("❌ Error distributing payment for order group {}: {}", 
                        orderGroup.getGroupId(), e.getMessage(), e);
                log.error("❌ Exception type: {}", e.getClass().getName());
                if (e.getCause() != null) {
                    log.error("❌ Caused by: {}", e.getCause().getMessage());
                }
                // Payment is still SUCCESS, distribution can be retried later via admin endpoint
                log.warn("⚠️ Payment status remains SUCCESS despite distribution error");
            }
            
        } else {
            log.warn("❌ Payment failed with status: {}", status);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed with status: " + status);
            
            // Release inventory reservations on payment failure
            if (payment.getOrderGroup() != null) {
                log.info("📦 Releasing inventory reservations due to payment failure");
                try {
                    inventoryService.releaseReservations(payment.getOrderGroup());
                    log.info("✅ Inventory reservations released");
                } catch (Exception e) {
                    log.error("❌ Error releasing inventory reservations: {}", e.getMessage(), e);
                }
            }
        }
        
        log.info("💾 Final save of payment record");
        payment = paymentRepository.save(payment);
        log.info("✅ Payment callback processing completed with final status: {}", payment.getStatus());
        log.info("========================================");
        
        return mapToPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getOrderGroupPayments(UUID orderGroupId, UUID customerId, String role) {
        log.info("Getting payments for order group: {} by user: {} with role: {}", orderGroupId, customerId, role);
        
        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            OrderGroup orderGroup = orderGroupRepository.findById(orderGroupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm đơn hàng"));
            if (!orderGroup.getCustomer().getAccountId().equals(customerId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán này");
            }
        }
        
        List<Payment> payments = paymentRepository.findByOrderGroup_GroupId(orderGroupId);
        
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse getPaymentById(UUID paymentId, UUID customerId, String role) {
        log.info("Getting payment: {} by user: {} with role: {}", paymentId, customerId, role);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));
        
        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            if (payment.getOrderGroup() == null || 
                !payment.getOrderGroup().getCustomer().getAccountId().equals(customerId)) {
                throw new BadRequestException("Bạn không có quyền xem thanh toán này");
            }
        }
        
        return mapToPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getUserPayments(UUID customerId, String role) {
        log.info("Getting payments for user: {} with role: {}", customerId, role);
        
        List<Payment> payments;
        
        if ("CUSTOMER".equals(role)) {
            // Get payments for customer's order groups
            payments = paymentRepository.findByCustomerId(customerId);
        } else if ("ADMIN".equals(role)) {
            // Admin can see all payments
            payments = paymentRepository.findAll();
        } else {
            payments = List.of();
        }
        
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isOrderGroupPaid(UUID orderGroupId) {
        return paymentRepository.isOrderGroupPaid(orderGroupId);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, String reason) {
        log.info("Refunding payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));
        
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Chỉ có thể hoàn tiền cho payment đã thành công");
        }
        
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setFailureReason("Refunded: " + reason);
        
        OrderGroup orderGroup = payment.getOrderGroup();
        if (orderGroup != null) {
            orderGroup.setStatus("CANCELLED");
            orderGroupRepository.save(orderGroup);
            
            // Cancel all orders in group
            for (Order order : orderGroup.getOrders()) {
                order.setStatus("CANCELED");
                order.setUpdateAt(LocalDateTime.now());
                orderRepository.save(order);
            }
        }
        
        payment = paymentRepository.save(payment);
        log.info("Payment refunded successfully: {}", paymentId);
        
        return mapToPaymentResponse(payment);
    }

    @Override
    public Payment findById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));
    }
    
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                .paymentStatus(payment.getStatus())
                .paymentMethod(payment.getMethod())
                .transactionId(payment.getTransactionId())
                .paymentUrl(payment.getPaymentUrl())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt());
        
        // CRITICAL: Don't access orderGroup directly to avoid circular reference
        // Use try-catch to safely get groupId
        try {
            if (payment.getOrderGroup() != null) {
                builder.orderGroupId(payment.getOrderGroup().getGroupId());
            }
        } catch (Exception e) {
            log.warn("Could not get orderGroupId for payment {}: {}", 
                    payment.getPaymentId(), e.getMessage());
            // Continue without orderGroupId
        }
        
        return builder.build();
    }
    
    /**
     * Auto-detect platform from return URL scheme
     * @param returnUrl The return URL
     * @return "web" for HTTP(S) URLs, "mobile" for custom schemes, null if returnUrl is null
     */
    private String detectPlatform(String returnUrl) {
        if (returnUrl == null || returnUrl.isEmpty()) {
            return null;
        }
        
        // Check if it's a standard web URL
        if (returnUrl.startsWith("http://") || returnUrl.startsWith("https://")) {
            return "web";
        }
        
        // Otherwise, assume it's a mobile deep link (e.g., catholicsouvenir://, myapp://)
        if (returnUrl.contains("://")) {
            return "mobile";
        }
        
        // Default to web if scheme is unclear
        return "web";
    }
}
