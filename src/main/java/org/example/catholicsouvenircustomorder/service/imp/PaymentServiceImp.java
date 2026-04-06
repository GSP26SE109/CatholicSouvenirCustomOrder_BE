package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.config.VNPayConfig;
import org.example.catholicsouvenircustomorder.config.ZaloPayConfig;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.PaymentCallbackRequest;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.NotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.example.catholicsouvenircustomorder.util.VNPayUtil;
import org.example.catholicsouvenircustomorder.util.ZaloPayUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final CustomOrderStageRepository stageRepository;
    private final ZaloPayConfig zaloPayConfig;
    private final VNPayConfig vnPayConfig;
    private final ZaloPayUtil zaloPayUtil;
    private final VNPayUtil vnPayUtil;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(InitiatePaymentDTO dto) {
        // Validate that exactly one ID is provided
        int idCount = 0;
        if (dto.getOrderId() != null) idCount++;
        if (dto.getCustomOrderId() != null) idCount++;
        if (dto.getStageId() != null) idCount++;
        
        if (idCount != 1) {
            throw new BadRequestException("Phải cung cấp chính xác một trong orderId, customOrderId hoặc stageId");
        }
        
        Payment payment = new Payment();
        payment.setMethod(dto.getMethod());
        payment.setStatus(PaymentStatus.PENDING);
        
        // Determine payment type and set amount
        if (dto.getOrderId() != null) {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
            payment.setOrder(order);
            payment.setAmount(order.getTotal());
        } else if (dto.getCustomOrderId() != null) {
            CustomOrder customOrder = customOrderRepository.findById(dto.getCustomOrderId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng tùy chỉnh"));
            
            if (customOrder.getStatus() != CustomOrderStatus.PENDING_PAYMENT) {
                throw new BadRequestException("Đơn hàng không ở trạng thái chờ thanh toán");
            }
            
            payment.setCustomOrder(customOrder);
            payment.setAmount(customOrder.getTotalPrice());
        } else {
            CustomOrderStage stage = stageRepository.findById(dto.getStageId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
            
            if (stage.getStatus() == StageStatus.PAID) {
                throw new BadRequestException("Giai đoạn này đã được thanh toán");
            }
            
            payment.setStage(stage);
            payment.setAmount(stage.getAmount());
        }
        
        payment = paymentRepository.save(payment);
        
        String paymentUrl;
        String transactionId;
        
        try {
            if (dto.getMethod() == PaymentMethod.VNPAY) {
                Map<String, Object> result = createVNPayOrder(payment, dto.getReturnUrl());
                paymentUrl = (String) result.get("payment_url");
                transactionId = (String) result.get("txn_ref");
            } else if (dto.getMethod() == PaymentMethod.ZALOPAY) {
                Map<String, Object> result = createZaloPayOrder(payment);
                paymentUrl = (String) result.get("order_url");
                transactionId = (String) result.get("app_trans_id");
            } else {
                throw new BadRequestException("Phương thức thanh toán không được hỗ trợ");
            }
            
            payment.setPaymentUrl(paymentUrl);
            payment.setTransactionId(transactionId);
            payment.setStatus(PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);
            
        } catch (Exception e) {
            log.error("Error creating payment", e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            throw new BadRequestException("Tạo thanh toán thất bại: " + e.getMessage());
        }
        
        return PaymentInitiationResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentUrl(paymentUrl)
                .amount(payment.getAmount())
                .transactionId(transactionId)
                .build();
    }

    private Map<String, Object> createVNPayOrder(Payment payment, String returnUrl) throws Exception {
        String txnRef = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(payment.getAmount().multiply(new BigDecimal("100")).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Payment " + payment.getPaymentId());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl != null ? returnUrl : vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", vnPayUtil.getVNPayDate());
        vnpParams.put("vnp_ExpireDate", vnPayUtil.getExpireDate(15));
        
        String queryUrl = vnPayUtil.buildQueryUrl(vnpParams);
        String secureHash = vnPayUtil.generateSecureHash(vnpParams, vnPayConfig.getHashSecret());
        
        String paymentUrl = vnPayConfig.getUrl() + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
        
        Map<String, Object> result = new HashMap<>();
        result.put("payment_url", paymentUrl);
        result.put("txn_ref", txnRef);
        return result;
    }
    
    private Map<String, Object> createZaloPayOrder(Payment payment) throws Exception {
        String appTransId = zaloPayUtil.generateAppTransId();
        String timestamp = zaloPayUtil.getCurrentTimestamp();
        
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("app_id", zaloPayConfig.getAppId());
        orderData.put("app_trans_id", appTransId);
        orderData.put("app_user", "user_" + payment.getPaymentId());
        orderData.put("app_time", Long.parseLong(timestamp));
        orderData.put("amount", payment.getAmount().longValue());
        orderData.put("item", "[{\"name\":\"Payment " + payment.getPaymentId() + "\"}]");
        orderData.put("description", "Payment " + payment.getPaymentId());
        orderData.put("embed_data", "{}");
        orderData.put("bank_code", "");
        orderData.put("callback_url", zaloPayConfig.getCallbackUrl());
        
        String data = zaloPayConfig.getAppId() + "|" + appTransId + "|" + 
                     "user_" + payment.getPaymentId() + "|" + payment.getAmount().longValue() + "|" +
                     timestamp + "|{}||";
        
        String mac = zaloPayUtil.generateMac(data, zaloPayConfig.getKey1());
        orderData.put("mac", mac);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderData, headers);
        
        Map<String, Object> response = restTemplate.postForObject(
            zaloPayConfig.getEndpoint() + "/create",
            request,
            Map.class
        );
        
        if (response != null && (Integer) response.get("return_code") == 1) {
            Map<String, Object> result = new HashMap<>();
            result.put("order_url", response.get("order_url"));
            result.put("app_trans_id", appTransId);
            return result;
        }
        
        throw new BadRequestException("Tạo đơn hàng ZaloPay thất bại");
    }

    @Override
    @Transactional
    public PaymentResponse handlePaymentCallback(PaymentCallbackRequest request) {
        Map<String, String> params = request.getParams();
        String gateway = request.getPaymentGateway();
        
        if ("VNPAY".equalsIgnoreCase(gateway)) {
            return handleVNPayCallback(params);
        } else if ("ZALOPAY".equalsIgnoreCase(gateway)) {
            return handleZaloPayCallback(params);
        } else {
            throw new BadRequestException("Cổng thanh toán không hợp lệ");
        }
    }
    
    private PaymentResponse handleVNPayCallback(Map<String, String> callbackData) {
        if (!vnPayUtil.verifySecureHash(callbackData, vnPayConfig.getHashSecret())) {
            throw new BadRequestException("Chữ ký callback không hợp lệ");
        }
        
        String txnRef = callbackData.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByTransactionId(txnRef)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thanh toán"));
        
        String responseCode = callbackData.get("vnp_ResponseCode");
        
        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            
            updateRelatedEntity(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("VNPay response code: " + responseCode);
        }
        
        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }
    
    private PaymentResponse handleZaloPayCallback(Map<String, String> callbackData) {
        if (!zaloPayUtil.verifyCallback(callbackData, zaloPayConfig.getKey2())) {
            throw new BadRequestException("Chữ ký callback không hợp lệ");
        }
        
        String appTransId = callbackData.get("app_trans_id");
        Payment payment = paymentRepository.findByTransactionId(appTransId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thanh toán"));
        
        int status = Integer.parseInt(callbackData.get("status"));
        
        if (status == 1) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            
            updateRelatedEntity(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("ZaloPay status: " + status);
        }
        
        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }
    
    private void updateRelatedEntity(Payment payment) {
        if (payment.getOrder() != null) {
            Order order = payment.getOrder();
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            
        } else if (payment.getCustomOrder() != null) {
            CustomOrder customOrder = payment.getCustomOrder();
            customOrder.setStatus(CustomOrderStatus.CONFIRMED);
            customOrderRepository.save(customOrder);
            
            // Send notification with error handling
            try {
                notificationService.notifyArtisanOfPaymentSuccess(
                    customOrder.getArtisan().getAccount().getAccountId(),
                    customOrder.getCustomOrderId(),
                    customOrder.getRequest().getCustomer().getFullName(),
                    payment.getAmount().longValue()
                );
            } catch (Exception e) {
                log.error("Failed to notify artisan of payment success for order {}: {}", 
                    customOrder.getCustomOrderId(), e.getMessage());
                // Payment is still successful, just log the notification error
            }
            
        } else if (payment.getStage() != null) {
            CustomOrderStage stage = payment.getStage();
            stage.setStatus(StageStatus.PAID);
            stage.setPaidAt(LocalDateTime.now());
            stageRepository.save(stage);
            
            // Send notification with error handling
            try {
                notificationService.notifyArtisanOfPayment(
                    stage.getCustomOrder().getArtisan().getAccount().getAccountId(),
                    stage.getStageId(),
                    stage.getName(),
                    payment.getAmount().longValue()
                );
            } catch (Exception e) {
                log.error("Failed to notify artisan of stage payment for stage {}: {}", 
                    stage.getStageId(), e.getMessage());
                // Payment is still successful, just log the notification error
            }
            
            CustomOrder customOrder = stage.getCustomOrder();
            List<CustomOrderStage> allStages = stageRepository.findByCustomOrder_CustomOrderIdOrderByStageOrderAsc(
                    customOrder.getCustomOrderId());
            
            boolean allStagesPaid = allStages.stream()
                    .allMatch(s -> s.getStatus() == StageStatus.PAID || 
                                   s.getStatus() == StageStatus.IN_PROGRESS || 
                                   s.getStatus() == StageStatus.COMPLETED);
            
            if (allStagesPaid && customOrder.getStatus() == CustomOrderStatus.PENDING_PAYMENT) {
                customOrder.setStatus(CustomOrderStatus.CONFIRMED);
                customOrderRepository.save(customOrder);
            }
        }
    }

    @Override
    public List<PaymentResponse> getOrderPayments(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
        
        return paymentRepository.findByOrder(order).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponse> getCustomOrderPayments(UUID customOrderId) {
        CustomOrder customOrder = customOrderRepository.findById(customOrderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng tùy chỉnh"));
        
        return paymentRepository.findByCustomOrder(customOrder).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponse> getStagePayments(UUID stageId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        return paymentRepository.findByStage(stage).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isOrderFullyPaid(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
        
        return paymentRepository.isOrderFullyPaid(order, order.getTotal());
    }

    @Override
    public boolean isCustomOrderFullyPaid(UUID customOrderId) {
        CustomOrder customOrder = customOrderRepository.findById(customOrderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng tùy chỉnh"));
        
        return paymentRepository.isCustomOrderFullyPaid(customOrder, customOrder.getTotalPrice());
    }

    @Override
    public boolean isStageFullyPaid(UUID stageId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        return paymentRepository.isStageFullyPaid(stage, stage.getAmount());
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thanh toán"));
        
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Chỉ có thể hoàn tiền cho thanh toán thành công");
        }
        
        try {
            // Call gateway refund API
            if (payment.getMethod() == PaymentMethod.VNPAY) {
                // VNPay refund logic would go here
                log.info("Processing VNPay refund for payment: {}", paymentId);
            } else if (payment.getMethod() == PaymentMethod.ZALOPAY) {
                // ZaloPay refund logic would go here
                log.info("Processing ZaloPay refund for payment: {}", paymentId);
            }
            
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setFailureReason(reason);
            payment = paymentRepository.save(payment);
            
            log.info("Payment {} refunded successfully", paymentId);
            
        } catch (Exception e) {
            log.error("Error processing refund", e);
            throw new BadRequestException("Hoàn tiền thất bại: " + e.getMessage());
        }
        
        return mapToResponse(payment);
    }

    @Override
    public Payment findById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thanh toán"));
    }
    
    private PaymentResponse mapToResponse(Payment payment) {
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
    }
}
