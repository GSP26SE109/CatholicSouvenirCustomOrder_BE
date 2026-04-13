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
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.PaymentRepository;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.example.catholicsouvenircustomorder.util.VNPayUtil;
import org.example.catholicsouvenircustomorder.util.ZaloPayUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for Order payments only
 * For custom order stage payments, use StagePaymentService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final VNPayUtil vnPayUtil;
    private final ZaloPayUtil zaloPayUtil;
    private final WalletServiceImp walletService;

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(InitiatePaymentDTO dto) {
        log.info("Initiating payment for order: {}", dto.getOrderId());
        
        if (dto.getOrderId() == null) {
            throw new BadRequestException("Order ID không được để trống");
        }
        
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        
        if ("PAID".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus())) {
            throw new BadRequestException("Đơn hàng đã được thanh toán");
        }
        
        Payment existingPayment = paymentRepository.findByOrderOrderIdAndStatus(
                dto.getOrderId(), PaymentStatus.PENDING
        ).orElse(null);
        
        if (existingPayment != null) {
            log.info("Returning existing pending payment: {}", existingPayment.getPaymentId());
            return PaymentInitiationResponse.builder()
                    .paymentId(existingPayment.getPaymentId())
                    .paymentUrl(existingPayment.getPaymentUrl())
                    .transactionId(existingPayment.getTransactionId())
                    .amount(existingPayment.getAmount())
                    .build();
        }
        
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(dto.getMethod());
        payment.setAmount(order.getTotal());
        payment.setStatus(PaymentStatus.PENDING);
        
        String referenceId = "ORDER_" + order.getOrderId() + "_" + System.currentTimeMillis();
        payment.setReferenceId(referenceId);
        
        String paymentUrl;
        try {
            // Use returnUrl from request, or fallback to config default
            String returnUrl = dto.getReturnUrl();
            
            if (dto.getMethod() == PaymentMethod.VNPAY) {
                paymentUrl = vnPayUtil.createPaymentUrl(
                        referenceId,
                        order.getTotal(),
                        "Thanh toán đơn hàng #" + order.getOrderId(),
                        order.getCustomer().getEmail(),
                        returnUrl  // Will use config default if null
                );
            } else if (dto.getMethod() == PaymentMethod.ZALOPAY) {
                paymentUrl = zaloPayUtil.createPaymentUrl(
                        referenceId,
                        order.getTotal(),
                        "Thanh toán đơn hàng #" + order.getOrderId(),
                        returnUrl  // Will use config default if null
                );
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
                .amount(order.getTotal())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse handlePaymentCallback(PaymentCallbackRequest request) {
        log.info("Handling payment callback for gateway: {}", request.getPaymentGateway());
        
        String referenceId;
        String gatewayTransactionId;
        String status;
        
        if ("VNPAY".equalsIgnoreCase(request.getPaymentGateway())) {
            referenceId = request.getParams().get("vnp_TxnRef");
            gatewayTransactionId = request.getParams().get("vnp_TransactionNo");
            status = request.getParams().get("vnp_ResponseCode");
        } else if ("ZALOPAY".equalsIgnoreCase(request.getPaymentGateway())) {
            referenceId = request.getParams().get("apptransid");
            gatewayTransactionId = request.getParams().get("zptransid");
            status = request.getParams().get("status");
        } else {
            throw new BadRequestException("Payment gateway không được hỗ trợ");
        }
        
        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));
        
        if ("00".equals(status) || "SUCCESS".equalsIgnoreCase(status) || "1".equals(status)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(gatewayTransactionId);
            payment.setPaidAt(LocalDateTime.now());
            
            Order order = payment.getOrder();
            if (order == null) {
                log.error("Payment {} has no associated order! Cannot process distribution.", payment.getPaymentId());
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("No order associated with payment");
            } else {
                order.setStatus(String.valueOf(OrderStatus.PAID));
                order.setUpdateAt(LocalDateTime.now());
                orderRepository.save(order);
                
                try {
                    Artisan artisan = findArtisanByOrder(order);
                    
                    if (artisan != null) {
                        Account platformAdmin = walletService.getPlatformAdminAccount();
                        walletService.processPaymentDistribution(payment, artisan, platformAdmin);
                        log.info("Payment distribution completed for order: {}", order.getOrderId());
                    } else {
                        log.error("No artisan found for order: {}", order.getOrderId());
                    }
                } catch (Exception e) {
                    log.error("Error distributing payment for order {}: {}", order.getOrderId(), e.getMessage(), e);
                }
                
                log.info("Payment successful for order: {}, gateway transaction: {}", 
                        order.getOrderId(), gatewayTransactionId);
            }
            
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed with status: " + status);
            log.warn("Payment failed for reference: {}", referenceId);
        }
        
        payment = paymentRepository.save(payment);
        
        return mapToPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getOrderPayments(UUID orderId) {
        log.info("Getting payments for order: {}", orderId);
        
        List<Payment> payments = paymentRepository.findByOrderOrderId(orderId);
        
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse getPaymentById(UUID paymentId) {
        log.info("Getting payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));
        
        return mapToPaymentResponse(payment);
    }

    @Override
    public boolean isOrderFullyPaid(UUID orderId) {
        return paymentRepository.isOrderFullyPaid(orderId);
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
        
        Order order = payment.getOrder();
        order.setStatus(String.valueOf(OrderStatus.CANCELED));
        order.setUpdateAt(LocalDateTime.now());
        orderRepository.save(order);
        
        payment = paymentRepository.save(payment);
        log.info("Payment refunded successfully: {}", paymentId);
        
        return mapToPaymentResponse(payment);
    }

    @Override
    public Payment findById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));
    }
    
    private Artisan findArtisanByOrder(Order order) {
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            Product product = order.getOrderDetails().get(0).getProduct();
            if (product != null && product.getArtisan() != null) {
                log.info("Found artisan from product order: {}", product.getArtisan().getArtisanUuid());
                return product.getArtisan();
            }
        }
        
        if (order.getTemplateDetails() != null && !order.getTemplateDetails().isEmpty()) {
            ProductTemplate template = order.getTemplateDetails().get(0).getTemplate();
            if (template != null && template.getArtisan() != null) {
                log.info("Found artisan from template order: {}", template.getArtisan().getArtisanUuid());
                return template.getArtisan();
            }
        }
        
        log.error("No artisan found for order: {}", order.getOrderId());
        return null;
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
        
        if (payment.getOrder() != null) {
            builder.orderId(payment.getOrder().getOrderId());
        }
        
        return builder.build();
    }
}
