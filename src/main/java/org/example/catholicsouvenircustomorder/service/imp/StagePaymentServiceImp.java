package org.example.catholicsouvenircustomorder.service.imp;

import org.example.catholicsouvenircustomorder.dto.response.StagePaymentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.StagePaymentRepository;
import org.example.catholicsouvenircustomorder.repository.CustomOrderStageRepository;
import org.example.catholicsouvenircustomorder.service.StagePaymentService;
import org.example.catholicsouvenircustomorder.util.VNPayUtil;
import org.example.catholicsouvenircustomorder.util.ZaloPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StagePaymentServiceImp implements StagePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StagePaymentServiceImp.class);

    @Autowired
    private StagePaymentRepository paymentRepository;

    @Autowired
    private CustomOrderStageRepository stageRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VNPayUtil vnPayUtil;

    @Autowired
    private ZaloPayUtil zaloPayUtil;

    @Autowired
    private WalletServiceImp walletService;

    @Override
    @Transactional
    public StagePaymentResponse createStagePayment(UUID stageId, UUID customerId, String paymentMethod) {
        log.info("Creating stage payment for stage: {}, customer: {}", stageId, customerId);

        // Validate stage
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy stage"));

        // Validate customer
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        // Validate stage belongs to customer's order
        if (!stage.getCustomOrder().getRequest().getCustomer().getAccountId().equals(customerId)) {
            throw new BadRequestException("Stage không thuộc về khách hàng này");
        }

        // Check if stage already paid
        if (stage.getStatus() == StageStatus.PAID || stage.getStatus() == StageStatus.COMPLETED) {
            throw new BadRequestException("Stage đã được thanh toán");
        }

        // Check if previous stages are paid (except for first stage)
        if (stage.getStageOrder() > 1) {
            boolean previousStagesPaid = stage.getCustomOrder().getStages().stream()
                    .filter(s -> s.getStageOrder() < stage.getStageOrder())
                    .allMatch(s -> s.getStatus() == StageStatus.PAID || s.getStatus() == StageStatus.COMPLETED);

            if (!previousStagesPaid) {
                throw new BadRequestException("Vui lòng thanh toán các stage trước đó");
            }
        }

        // Check if pending payment already exists for this stage
        StagePayment existingPayment = paymentRepository.findByStage_StageIdAndStatus(stageId, PaymentStatus.PENDING)
                .orElse(null);

        if (existingPayment != null) {
            return mapToPaymentResponse(existingPayment);
        }

        // Create payment
        StagePayment payment = new StagePayment();
        payment.setStage(stage);
        payment.setAmount(stage.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        payment.setPaymentType(determinePaymentType(stage));

        // Generate reference ID for internal tracking
        String referenceId = "STAGE_" + stageId + "_" + System.currentTimeMillis();
        payment.setReferenceId(referenceId);
        // transactionId will be set later from gateway callback
        String paymentUrl;

        try {
            // Stage payment callback URL
            String stageCallbackUrl = "http://localhost:8080/api/stage-payments/vnpay/callback";
            
            if (paymentMethod.equalsIgnoreCase("VNPAY")) {
                paymentUrl = vnPayUtil.createPaymentUrl(
                        referenceId,  // Use referenceId as vnp_TxnRef
                        stage.getAmount(),
                        "Thanh toán stage: " + stage.getName(),
                        customer.getEmail(),
                        stageCallbackUrl  // Use stage-specific callback URL
                );
            } else if (paymentMethod.equalsIgnoreCase("ZALOPAY")) {
                String zaloCallbackUrl = "http://localhost:8080/api/stage-payments/zalopay/callback";
                paymentUrl = zaloPayUtil.createPaymentUrl(
                        referenceId,  // Use referenceId as apptransid
                        stage.getAmount(),
                        "Thanh toán stage: " + stage.getName(),
                        zaloCallbackUrl  // Use stage-specific callback URL
                );
            } else {
                throw new BadRequestException("Phương thức thanh toán không hợp lệ");
            }

            payment.setPaymentUrl(paymentUrl);

        } catch (Exception e) {
            log.error("Error creating payment URL: ", e);
            throw new BadRequestException("Không thể tạo URL thanh toán: " + e.getMessage());
        }

        payment = paymentRepository.save(payment);
        log.info("Stage payment created successfully: {}", payment.getPaymentId());

        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional
    public StagePaymentResponse handleStagePaymentCallback(String referenceId, String status) {
        log.info("Handling stage payment callback for reference: {}, status: {}", referenceId, status);

        // Find payment by our reference ID
        StagePayment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));

        if (status.equalsIgnoreCase("SUCCESS") || status.equals("00")) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            // Note: transactionId from gateway should be set by the caller
            // or extracted from callback params before calling this method

            // Update stage status and workflow flags
            CustomOrderStage stage = payment.getStage();
            stage.setStatus(StageStatus.PAID);
            stage.setPaidAt(LocalDateTime.now());
            stage.setIsPaid(true);  // ← Set workflow flag
            stage.setCanPay(false); // ← Lock this stage (already paid)
            stageRepository.save(stage);

            // Distribute money: 90% to artisan, 10% platform fee
            Artisan artisan = findArtisanByStage(stage);
            
            if (artisan != null) {
                Account platformAdmin = walletService.getPlatformAdminAccount();
                walletService.processStagePaymentDistribution(payment, artisan, platformAdmin);
            } else {
                log.warn("No artisan found for stage: {}", stage.getStageId());
            }

            // Check if all stages are paid, update custom order status
            CustomOrder customOrder = stage.getCustomOrder();
            boolean allStagesPaid = customOrder.getStages().stream()
                    .allMatch(s -> s.getIsPaid());

            if (allStagesPaid) {
                customOrder.setStatus(CustomOrderStatus.IN_PROGRESS);
            } else {
                customOrder.setStatus(CustomOrderStatus.IN_PROGRESS);
            }

            log.info("Stage payment successful for stage: {}", stage.getStageId());

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed with status: " + status);
            log.warn("Stage payment failed for reference: {}", referenceId);
        }

        payment = paymentRepository.save(payment);
        return mapToPaymentResponse(payment);
    }

    @Override
    public StagePaymentResponse getSuccessfulStagePayment(UUID stageId) {
        StagePayment payment = paymentRepository.findFirstByStage_StageIdAndStatusOrderByPaidAtDesc(stageId, PaymentStatus.SUCCESS)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment thành công cho stage này"));

        return mapToPaymentResponse(payment);
    }

    @Override
    public List<StagePaymentResponse> getStagePaymentHistory(UUID stageId) {
        List<StagePayment> payments = paymentRepository.findByStage_StageIdOrderByCreatedAtDesc(stageId);
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isStagePaymentCompleted(UUID stageId) {
        return paymentRepository.findByStage_StageIdAndStatus(stageId, PaymentStatus.SUCCESS)
                .isPresent();
    }

    private PaymentType determinePaymentType(CustomOrderStage stage) {
        if (stage.getStageOrder() == 1) {
            return PaymentType.DEPOSIT_PAYMENT;
        }

        int totalStages = stage.getCustomOrder().getStages().size();
        if (stage.getStageOrder() == totalStages) {
            return PaymentType.FINAL_PAYMENT;
        }

        return PaymentType.PROGRESS_PAYMENT;
    }
    
    /**
     * Helper method to find artisan from custom order stage
     * In custom order, artisan is retrieved from CustomRequest.selectedArtisan
     */
    private Artisan findArtisanByStage(CustomOrderStage stage) {
        if (stage == null || stage.getCustomOrder() == null || 
            stage.getCustomOrder().getRequest() == null) {
            log.warn("Invalid stage or custom order structure");
            return null;
        }
        
        return stage.getCustomOrder().getRequest().getSelectedArtisan();
    }

    private StagePaymentResponse mapToPaymentResponse(StagePayment payment) {
        return StagePaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                .paymentStatus(payment.getStatus())
                .paymentMethod(payment.getMethod())
                .paymentType(payment.getPaymentType())
                .transactionId(payment.getTransactionId())
                .paymentUrl(payment.getPaymentUrl())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .stageId(payment.getStage() != null ? payment.getStage().getStageId() : null)
                .stageName(payment.getStage() != null ? payment.getStage().getName() : null)
                .stageOrder(payment.getStage() != null ? payment.getStage().getStageOrder() : null)
                .build();
    }
}
