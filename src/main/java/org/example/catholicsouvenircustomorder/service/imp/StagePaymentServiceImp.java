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
            existingPayment.setStatus(PaymentStatus.CANCELLED);
            existingPayment.setFailureReason("Replaced by new payment request");
            paymentRepository.save(existingPayment);
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
    @Transactional(noRollbackFor = Exception.class)
    public StagePaymentResponse handleStagePaymentCallback(String referenceId, String status) {
        log.info("========================================");
        log.info("Processing payment callback - referenceId: {}, status: {}", referenceId, status);
        
        // Find payment by our reference ID
        StagePayment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> {
                    log.error("Payment not found for referenceId: {}", referenceId);
                    return new ResourceNotFoundException("Không tìm thấy payment với referenceId: " + referenceId);
                });
        
        log.info("Found payment: paymentId={}, currentStatus={}, amount={}", 
                payment.getPaymentId(), payment.getStatus(), payment.getAmount());

        if (status.equalsIgnoreCase("SUCCESS") || status.equals("00")) {
            log.info("Processing SUCCESS payment");
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            // Update stage status and workflow flags
            CustomOrderStage stage = payment.getStage();
            stage.setStatus(StageStatus.PAID);
            stage.setPaidAt(LocalDateTime.now());
            stage.setIsPaid(true);
            stage.setCanPay(false);
            stageRepository.save(stage);

            // Distribute money: 90% to artisan, 10% platform fee
            try {
                log.info("Starting payment distribution for paymentId: {}", payment.getPaymentId());
                
                StagePayment paymentForDistribution = paymentRepository
                        .findByIdWithDetailsForDistribution(payment.getPaymentId())
                        .orElse(payment);
                
                log.info("Fetched payment for distribution");
                
                Artisan artisan = findArtisanByStage(stage);
                
                if (artisan != null) {
                    log.info("Found artisan: artisanId={}, accountId={}", 
                            artisan.getArtisanUuid(), artisan.getAccount().getAccountId());
                    // Lấy commission_rate từ StagePayment entity (đã lưu trước đó)
                    BigDecimal commissionRate = payment.getCommissionRate();
                    
                    // Tính commission và net amount
                    CommissionCalculation calc = commissionService.calculateCommission(
                        payment.getAmount(),
                        commissionRate
                    );
                    
                    log.info("Stage payment {}: Original={}, Commission={}, Net={}", 
                        payment.getPaymentId(), calc.getOriginalAmount(), 
                        calc.getCommissionAmount(), calc.getNetAmount());
                    
                    // Cộng NET AMOUNT vào wallet (đã trừ commission)
                    Account platformAdmin = walletService.getPlatformAdminAccount();
                    log.info("Got platform admin account: {}", platformAdmin.getAccountId());
                    
                    // Create a custom distribution that adds net amount to artisan wallet
                    // and applies commission to the wallet transaction
                    log.info("Starting wallet distribution...");
                    WalletTransaction walletTx = distributeStagePaymentWithCommission(
                        paymentForDistribution, 
                        artisan, 
                        platformAdmin,
                        calc
                    );
                    log.info("Wallet distribution completed. TransactionId: {}", walletTx.getTransactionId());
                    
                    // Gửi notification cho artisan về commission
                    try {
                        UUID customOrderId = stage.getCustomOrder().getCustomOrderId();
                        log.info("Sending commission notification to artisan...");
                        notificationService.notifyArtisanCommissionDeducted(
                            artisan.getArtisanUuid(),
                            customOrderId,
                            calc.getOriginalAmount(),
                            calc.getCommissionAmount(),
                            calc.getNetAmount(),
                            walletTx.getTransactionId()
                        );
                        log.info("Commission notification sent successfully");
                    } catch (Exception e) {
                        log.error("Failed to send commission notification: {}", e.getMessage(), e);
                        // Continue processing even if notification fails
                    }
                } else {
                    log.warn("No artisan found for stage: {}, skipping distribution", stage.getStageId());
                }
            } catch (Exception e) {
                log.error("Error distributing stage payment for paymentId: {}", payment.getPaymentId(), e);
                log.error("Distribution error details - type: {}, message: {}", 
                        e.getClass().getName(), e.getMessage());
                if (e.getCause() != null) {
                    log.error("Caused by: {}", e.getCause().getMessage());
                }
            }

            // Update custom order status
            CustomOrder customOrder = stage.getCustomOrder();
            log.info("Checking if all stages are paid for customOrderId: {}", customOrder.getCustomOrderId());
            
            boolean allStagesPaid = customOrder.getStages().stream()
                    .allMatch(s -> s.getIsPaid());

            log.info("All stages paid: {}", allStagesPaid);
            
            if (allStagesPaid) {
                customOrder.setStatus(CustomOrderStatus.IN_PROGRESS);
                log.info("Updated custom order status to IN_PROGRESS (all stages paid)");
            } else {
                customOrder.setStatus(CustomOrderStatus.IN_PROGRESS);
                log.info("Updated custom order status to IN_PROGRESS (partial payment)");
            }

        } else {
            log.info("Processing FAILED payment");
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed with status: " + status);
        }

        log.info("Saving payment with final status: {}", payment.getStatus());
        payment = paymentRepository.save(payment);
        
        log.info("Payment callback processing completed successfully");
        log.info("========================================");
        
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
     * Distribute stage payment with commission deduction
     * This method handles the wallet distribution after commission calculation
     */
    private WalletTransaction distributeStagePaymentWithCommission(StagePayment stagePayment, 
                                                     Artisan artisan, 
                                                     Account platformAdmin,
                                                     CommissionCalculation calc) {
        // Use the new wallet service method that supports commission
        WalletTransaction walletTx = walletService.processStagePaymentDistributionWithCommission(
            stagePayment, 
            artisan, 
            platformAdmin,
            calc.getCommissionAmount(),
            stagePayment.getCommissionRate()
        );
        
        log.info("Stage payment distribution completed with commission tracking: Original={}, Commission={}, Net={}", 
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
