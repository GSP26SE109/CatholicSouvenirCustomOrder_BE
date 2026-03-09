package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.config.VNPayConfig;
import org.example.catholicsouvenircustomorder.config.ZaloPayConfig;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.NotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.CustomOrderStageRepository;
import org.example.catholicsouvenircustomorder.repository.StagePaymentRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService {
    
    private final StagePaymentRepository stagePaymentRepository;
    private final CustomOrderStageRepository stageRepository;
    private final AccountRepository accountRepository;
    private final ZaloPayConfig zaloPayConfig;
    private final VNPayConfig vnPayConfig;
    private final ZaloPayUtil zaloPayUtil;
    private final VNPayUtil vnPayUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String createPaymentByPayOS(UUID orderId, UUID stageId) {
        return "";
    }

    @Override
    public Payment findById(UUID paymentId) {
        return null;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(UUID stageId, PaymentMethod method, UUID customerId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        // Get customer from stage's order if customerId is not provided
        Account customer;
        if (customerId != null) {
            customer = accountRepository.findById(customerId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng"));
        } else {
            customer = stage.getCustomOrder().getCustomer();
        }
        
        if (stage.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Giai đoạn này đã được thanh toán");
        }
        
        // Validate sequential payment: previous stage must be completed
        if (stage.getStageOrder() > 1) {
            List<CustomOrderStage> allStages = stageRepository
                    .findByCustomOrder_OrderIdOrderByStageOrderAsc(stage.getCustomOrder().getOrderId());
            
            CustomOrderStage previousStage = allStages.stream()
                    .filter(s -> s.getStageOrder().equals(stage.getStageOrder() - 1))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn trước"));
            
            if (previousStage.getCompletedAt() == null) {
                throw new BadRequestException("Không thể thanh toán giai đoạn này. Giai đoạn trước phải được hoàn thành trước");
            }
            
            if (previousStage.getPaymentStatus() != PaymentStatus.SUCCESS) {
                throw new BadRequestException("Không thể thanh toán giai đoạn này. Giai đoạn trước phải được thanh toán trước");
            }
        }
        
        StagePayment payment = new StagePayment();
        payment.setStage(stage);
        payment.setCustomer(customer);
        payment.setPaymentMethod(method);
        payment.setAmount(stage.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        
        payment = stagePaymentRepository.save(payment);
        
        String paymentUrl;
        String gatewayOrderId;
        
        try {
            if (method == PaymentMethod.ZALOPAY) {
                Map<String, Object> result = createZaloPayOrder(payment);
                paymentUrl = (String) result.get("order_url");
                gatewayOrderId = (String) result.get("app_trans_id");
            } else {
                Map<String, Object> result = createVNPayOrder(payment);
                paymentUrl = (String) result.get("payment_url");
                gatewayOrderId = (String) result.get("txn_ref");
            }
            
            payment.setPaymentUrl(paymentUrl);
            payment.setGatewayOrderId(gatewayOrderId);
            payment.setStatus(PaymentStatus.PROCESSING);
            payment = stagePaymentRepository.save(payment);
            
        } catch (Exception e) {
            log.error("Error creating payment", e);
            payment.setStatus(PaymentStatus.FAILED);
            stagePaymentRepository.save(payment);
            throw new BadRequestException("Tạo thanh toán thất bại: " + e.getMessage());
        }
        
        return mapToResponse(payment);
    }

    private Map<String, Object> createZaloPayOrder(StagePayment payment) throws Exception {
        String appTransId = zaloPayUtil.generateAppTransId();
        String timestamp = zaloPayUtil.getCurrentTimestamp();
        
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("app_id", zaloPayConfig.getAppId());
        orderData.put("app_trans_id", appTransId);
        orderData.put("app_user", payment.getCustomer().getEmail());
        orderData.put("app_time", Long.parseLong(timestamp));
        orderData.put("amount", payment.getAmount().longValue());
        orderData.put("item", "[{\"name\":\"" + payment.getStage().getName() + "\"}]");
        orderData.put("description", "Payment for " + payment.getStage().getName());
        orderData.put("embed_data", "{}");
        orderData.put("bank_code", "");
        orderData.put("callback_url", zaloPayConfig.getCallbackUrl());
        
        String data = zaloPayConfig.getAppId() + "|" + appTransId + "|" + 
                     payment.getCustomer().getEmail() + "|" + payment.getAmount().longValue() + "|" +
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
    
    private Map<String, Object> createVNPayOrder(StagePayment payment) throws Exception {
        String txnRef = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(payment.getAmount().multiply(new BigDecimal("100")).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Payment for " + payment.getStage().getName());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
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
    
    @Override
    @Transactional
    public PaymentResponse handleZaloPayCallback(Map<String, String> callbackData) {
        if (!zaloPayUtil.verifyCallback(callbackData, zaloPayConfig.getKey2())) {
            throw new BadRequestException("Chữ ký callback không hợp lệ");
        }
        
        String appTransId = callbackData.get("app_trans_id");
        StagePayment payment = stagePaymentRepository.findAll().stream()
                .filter(p -> appTransId.equals(p.getGatewayOrderId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thanh toán"));
        
        int status = Integer.parseInt(callbackData.get("status"));
        
        if (status == 1) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(callbackData.get("zp_trans_id"));
            
            CustomOrderStage stage = payment.getStage();
            stage.setPaymentStatus(PaymentStatus.SUCCESS);
            stage.setPaidAt(LocalDateTime.now());
            stageRepository.save(stage);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        
        payment.setGatewayResponse(callbackData.toString());
        payment = stagePaymentRepository.save(payment);
        
        return mapToResponse(payment);
    }
    
    @Override
    @Transactional
    public PaymentResponse handleVNPayCallback(Map<String, String> callbackData) {
        if (!vnPayUtil.verifySecureHash(callbackData, vnPayConfig.getHashSecret())) {
            throw new BadRequestException("Chữ ký callback không hợp lệ");
        }
        
        String txnRef = callbackData.get("vnp_TxnRef");
        StagePayment payment = stagePaymentRepository.findAll().stream()
                .filter(p -> txnRef.equals(p.getGatewayOrderId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thanh toán"));
        
        String responseCode = callbackData.get("vnp_ResponseCode");
        
        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(callbackData.get("vnp_TransactionNo"));
            
            CustomOrderStage stage = payment.getStage();
            stage.setPaymentStatus(PaymentStatus.SUCCESS);
            stage.setPaidAt(LocalDateTime.now());
            stageRepository.save(stage);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        
        payment.setGatewayResponse(callbackData.toString());
        payment = stagePaymentRepository.save(payment);
        
        return mapToResponse(payment);
    }
    
    @Override
    public PaymentResponse getPaymentByStageId(UUID stageId) {
        List<StagePayment> payments = stagePaymentRepository.findByStage_StageId(stageId);
        if (payments.isEmpty()) {
            throw new NotFoundException("Thanh toán không tồn tại");
        }
        return mapToResponse(payments.get(0));
    }
    
    private PaymentResponse mapToResponse(StagePayment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .stageId(payment.getStage().getStageId())
                .stageName(payment.getStage().getName())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .gatewayOrderId(payment.getGatewayOrderId())
                .paymentUrl(payment.getPaymentUrl())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
