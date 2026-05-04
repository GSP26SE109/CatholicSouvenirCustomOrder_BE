package org.example.catholicsouvenircustomorder.service.imp;

import org.example.catholicsouvenircustomorder.dto.CommissionCalculation;
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

import java.math.BigDecimal;
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
    
    @Autowired
    private org.example.catholicsouvenircustomorder.repository.ArtisanRepository artisanRepository;
    
    @Autowired
    private org.example.catholicsouvenircustomorder.config.VNPayConfig vnPayConfig;
    
    @Autowired
    private SystemConfigServiceImp systemConfigService;
    
    @Autowired
    private CommissionServiceImp commissionService;
    
    @Autowired
    private NotificationServiceImp notificationService;

    @Override
    @Transactional
    public StagePaymentResponse createStagePayment(UUID stageId, UUID customerId, String paymentMethod) {
        return createStagePayment(stageId, customerId, paymentMethod, null);
    }
    
    @Transactional
    public StagePaymentResponse createStagePayment(UUID stageId, UUID customerId, String paymentMethod, String returnUrl) {
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
            log.warn("⚠️ Stage {} is already PAID, cannot create new payment", stageId);
            throw new BadRequestException("Stage đã được thanh toán");
        }
        
        // Check if already has successful payment
        boolean hasSuccessfulPayment = paymentRepository.findByStage_StageIdAndStatus(
                stageId, PaymentStatus.SUCCESS
        ).isPresent();
        
        if (hasSuccessfulPayment) {
            log.warn("⚠️ Stage {} already has successful payment", stageId);
            throw new BadRequestException("Stage này đã được thanh toán");
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
            // Check if existing payment is recent (within 15 minutes)
            LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
            if (existingPayment.getCreatedAt().isAfter(fifteenMinutesAgo)) {
                // Payment is recent, reuse it
                log.info("⚠️ Found recent pending payment (created {}), reusing it", 
                        existingPayment.getCreatedAt());
                
                return mapToPaymentResponse(existingPayment);
            } else {
                // Payment is old, cancel it
                log.info("Found old pending payment: {} (created {}), cancelling it", 
                        existingPayment.getPaymentId(), existingPayment.getCreatedAt());
                existingPayment.setStatus(PaymentStatus.CANCELLED);
                existingPayment.setFailureReason("Replaced by new payment request (expired)");
                paymentRepository.save(existingPayment);
                log.info("Old payment cancelled, creating new one");
            }
        }

        // Create payment
        StagePayment payment = new StagePayment();
        payment.setStage(stage);
        payment.setAmount(stage.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        payment.setPaymentType(determinePaymentType(stage));
        payment.setReturnUrl(returnUrl);
        
        // TỰ ĐỘNG lấy commission rate từ SystemConfigService
        BigDecimal commissionRate = systemConfigService.getCommissionRate();
        payment.setCommissionRate(commissionRate);
        log.info("StagePayment created with commission_rate={}%", commissionRate);

        // Generate reference ID for internal tracking
        String referenceId = "STAGE_" + stageId + "_" + System.currentTimeMillis();
        payment.setReferenceId(referenceId);
        
        String paymentUrl;

        try {
            String backendReturnUrl = vnPayConfig.getStageReturnUrl();
            
            if (paymentMethod.equalsIgnoreCase("VNPAY")) {
                paymentUrl = vnPayUtil.createPaymentUrl(
                        referenceId,
                        stage.getAmount(),
                        "Thanh toán stage: " + stage.getName(),
                        customer.getEmail(),
                        backendReturnUrl
                );
            } else if (paymentMethod.equalsIgnoreCase("ZALOPAY")) {
                String zaloCallbackUrl = "http://localhost:8080/api/stage-payments/zalopay/callback";
                paymentUrl = zaloPayUtil.createPaymentUrl(
                        referenceId,
                        stage.getAmount(),
                        "Thanh toán stage: " + stage.getName(),
                        zaloCallbackUrl
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
        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public StagePaymentResponse handleStagePaymentCallback(String referenceId, String status) {
        log.info("========================================");
        log.info("🔔 Processing stage payment callback");
        log.info("ReferenceId: {}, Status: {}", referenceId, status);

        // Find payment by our reference ID
        StagePayment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> {
                    log.error("❌ Payment not found for referenceId: {}", referenceId);
                    return new ResourceNotFoundException("Không tìm thấy payment với referenceId: " + referenceId);
                });
        
        log.info("✅ Found payment: paymentId={}, currentStatus={}, amount={}", 
                payment.getPaymentId(), payment.getStatus(), payment.getAmount());
        
        // IDEMPOTENCY CHECK
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("⚠️ Payment already processed successfully, returning existing result");
            return mapToPaymentResponse(payment);
        }
        
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.info("⚠️ Payment already marked as failed, returning existing result");
            return mapToPaymentResponse(payment);
        }

        if (status.equalsIgnoreCase("SUCCESS") || status.equals("00")) {
            log.info("✅ Payment successful, updating status");
            
            // CRITICAL: Update payment status FIRST in separate transaction
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            payment = paymentRepository.saveAndFlush(payment); // Force immediate commit
            log.info("✅ Payment saved with SUCCESS status: {}", payment.getStatus());

            // Update stage status
            CustomOrderStage stage = payment.getStage();
            log.info("📝 Updating stage status to PAID");
            stage.setStatus(StageStatus.PAID);
            stage.setPaidAt(LocalDateTime.now());
            stage.setIsPaid(true);
            stage.setCanPay(false);
            stageRepository.saveAndFlush(stage); // Force immediate commit
            log.info("✅ Stage {} status updated to PAID", stage.getStageId());

            // Update custom order status
            CustomOrder customOrder = stage.getCustomOrder();
            boolean allStagesPaid = customOrder.getStages().stream()
                    .allMatch(s -> s.getIsPaid());
            
            if (allStagesPaid) {
                customOrder.setStatus(CustomOrderStatus.IN_PROGRESS);
                log.info("✅ Updated custom order status to IN_PROGRESS (all stages paid)");
            } else {
                customOrder.setStatus(CustomOrderStatus.IN_PROGRESS);
                log.info("✅ Updated custom order status to IN_PROGRESS (partial payment)");
            }

            // Distribute money AFTER status is committed (use @Async to avoid transaction conflict)
            log.info("💰 Scheduling async payment distribution");
            try {
                // Call async method - this will run in separate thread pool
                distributePaymentAsync(payment.getPaymentId());
            } catch (Exception e) {
                log.error("Error scheduling distribution: {}", e.getMessage(), e);
                // Don't fail the callback - distribution can be retried
            }

        } else {
            log.warn("❌ Payment failed with status: {}", status);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed with status: " + status);
            payment = paymentRepository.saveAndFlush(payment);
        }

        log.info("✅ Stage payment callback processing completed with final status: {}", payment.getStatus());
        log.info("========================================");
        
        return mapToPaymentResponse(payment);
    }
    
    /**
     * Distribute payment in separate transaction to avoid blocking callback
     * MUST use @Async to run in separate thread and avoid transaction conflict
     */
    @org.springframework.scheduling.annotation.Async
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void distributePaymentAsync(UUID paymentId) {
        log.info("💰 Starting async payment distribution for paymentId: {}", paymentId);
        
        try {
            StagePayment payment = paymentRepository
                    .findByIdWithDetailsForDistribution(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
            
            CustomOrderStage stage = payment.getStage();
            Artisan artisan = findArtisanByStage(stage);
            
            if (artisan != null) {
                log.info("Found artisan: artisanId={}, accountId={}", 
                        artisan.getArtisanUuid(), artisan.getAccount().getAccountId());
                
                BigDecimal commissionRate = payment.getCommissionRate();
                CommissionCalculation calc = commissionService.calculateCommission(
                    payment.getAmount(),
                    commissionRate
                );
                
                log.info("Stage payment {}: Original={}, Commission={}, Net={}", 
                    payment.getPaymentId(), calc.getOriginalAmount(), 
                    calc.getCommissionAmount(), calc.getNetAmount());
                
                Account platformAdmin = walletService.getPlatformAdminAccount();
                
                WalletTransaction walletTx = distributeStagePaymentWithCommission(
                    payment, 
                    artisan, 
                    platformAdmin,
                    calc
                );
                log.info("✅ Wallet distribution completed. TransactionId: {}", walletTx.getTransactionId());
                
                // Send notification
                try {
                    UUID customOrderId = stage.getCustomOrder().getCustomOrderId();
                    notificationService.notifyArtisanCommissionDeducted(
                        artisan.getArtisanUuid(),
                        customOrderId,
                        calc.getOriginalAmount(),
                        calc.getCommissionAmount(),
                        calc.getNetAmount(),
                        walletTx.getTransactionId()
                    );
                    log.info("✅ Commission notification sent");
                } catch (Exception e) {
                    log.error("Failed to send notification: {}", e.getMessage());
                }
            } else {
                log.warn("No artisan found for stage: {}", stage.getStageId());
            }
        } catch (Exception e) {
            log.error("❌ Error in async distribution: {}", e.getMessage(), e);
            // Log but don't throw - payment status is already saved
        }
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
    
    @Override
    public String getReturnUrlByReferenceId(String referenceId) {
        return paymentRepository.findByReferenceId(referenceId)
                .map(StagePayment::getReturnUrl)
                .orElse(null);
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
     * Uses safe navigation to avoid lazy loading issues
     */
    private Artisan findArtisanByStage(CustomOrderStage stage) {
        if (stage == null) {
            log.warn("Stage is null");
            return null;
        }
        
        try {
            // Try to get artisan through the relationship chain
            if (stage.getCustomOrder() != null && 
                stage.getCustomOrder().getRequest() != null &&
                stage.getCustomOrder().getRequest().getSelectedArtisan() != null) {
                return stage.getCustomOrder().getRequest().getSelectedArtisan();
            }
        } catch (Exception e) {
            log.warn("Could not navigate to artisan through relationships: {}", e.getMessage());
        }
        
        // Fallback: Query artisan by custom order ID
        try {
            UUID customOrderId = stage.getCustomOrder().getCustomOrderId();
            return artisanRepository.findByCustomOrderId(customOrderId).orElse(null);
        } catch (Exception e) {
            log.error("Failed to find artisan for stage {}: {}", stage.getStageId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Distribute stage payment with commission deduction and 70/30 split
     * This method handles the wallet distribution after commission calculation
     * 70% is immediately available for withdrawal, 30% is locked until stage completion + 3 days
     */
    private WalletTransaction distributeStagePaymentWithCommission(StagePayment stagePayment, 
                                                     Artisan artisan, 
                                                     Account platformAdmin,
                                                     CommissionCalculation calc) {
        // Use the new wallet service method that supports commission and 70/30 split
        WalletTransaction walletTx = walletService.processStagePaymentDistributionWithCommission(
            stagePayment, 
            artisan, 
            platformAdmin,
            calc.getCommissionAmount(),
            stagePayment.getCommissionRate()
        );
        
        log.info("Stage payment distribution completed with commission tracking and 70/30 split: Original={}, Commission={}, Net={}, Available=70%, Locked=30%", 
                calc.getOriginalAmount(), calc.getCommissionAmount(), calc.getNetAmount());
        
        return walletTx;
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
