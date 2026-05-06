package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.response.CancellationEstimate;
import org.example.catholicsouvenircustomorder.dto.response.StageRefundCalculation;
import org.example.catholicsouvenircustomorder.exception.CancellationException;
import org.example.catholicsouvenircustomorder.exception.InsufficientBalanceException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.exception.UnauthorizedException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.dto.response.VNPayRefundResponse;
import org.example.catholicsouvenircustomorder.exception.RefundProcessingException;
import org.example.catholicsouvenircustomorder.repository.CustomOrderRepository;
import org.example.catholicsouvenircustomorder.repository.RefundTransactionRepository;
import org.example.catholicsouvenircustomorder.repository.StagePaymentRepository;
import org.example.catholicsouvenircustomorder.repository.WalletRepository;
import org.example.catholicsouvenircustomorder.service.CancellationService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.RefundCalculationService;
import org.example.catholicsouvenircustomorder.util.VNPayErrorMapper;
import org.example.catholicsouvenircustomorder.util.VNPayUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of CancellationService
 * Handles order cancellation with refund processing
 * Requirements: 10.1, 10.2, 2.1, 2.2, 2.3, 2.5, 2.6, 2.7, 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CancellationServiceImp implements CancellationService {
    
    private final CustomOrderRepository customOrderRepository;
    private final RefundCalculationService refundCalculationService;
    private final RefundTransactionRepository refundTransactionRepository;
    private final WalletRepository walletRepository;
    private final StagePaymentRepository stagePaymentRepository;
    private final VNPayUtil vnPayUtil;
    private final NotificationService notificationService;
    
    private static final int MIN_REASON_LENGTH = 20;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    @Override
    @Transactional
    public RefundTransaction cancelOrder(
        UUID customOrderId,
        UUID initiatedBy,
        CancellationInitiator initiator,
        String reason
    ) {
        log.info("Processing cancellation for order {}, initiator: {}, by account: {}", 
            customOrderId, initiator, initiatedBy);
        
        // 1. Get order
        CustomOrder order = customOrderRepository.findById(customOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        
        // 2. Validate cancellation (subtask 4.2)
        validateCancellation(order, initiatedBy, initiator, reason);
        
        // 3. Calculate refund amounts
        List<StageRefundCalculation> stageRefunds = refundCalculationService
            .calculateStageRefunds(order, initiator);
        
        if (stageRefunds.isEmpty()) {
            throw new CancellationException(
                "Không có stage nào đã thanh toán để hoàn tiền",
                CancellationException.NO_PAID_STAGES
            );
        }
        
        // Calculate totals
        BigDecimal grossRefundAmount = stageRefunds.stream()
            .map(StageRefundCalculation::getGrossRefund)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal platformCommission = stageRefunds.stream()
            .map(StageRefundCalculation::getPlatformCommission)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netRefundAmount = stageRefunds.stream()
            .map(StageRefundCalculation::getNetRefund)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Refund calculation: gross={}, commission={}, net={}", 
            grossRefundAmount, platformCommission, netRefundAmount);
        
        // 4. Check Artisan balance (subtask 4.3)
        Wallet artisanWallet = order.getArtisan().getWallet();
        BigDecimal availableBalance = artisanWallet.getAvailableBalance();
        
        if (availableBalance.compareTo(netRefundAmount) < 0) {
            log.error("Insufficient balance: available={}, required={}", 
                availableBalance, netRefundAmount);
            throw new InsufficientBalanceException(
                String.format("Artisan không đủ số dư để hoàn tiền. Cần: %s VND, Có: %s VND",
                    netRefundAmount, availableBalance)
            );
        }
        
        // 5. Deduct from Artisan wallet
        artisanWallet.setBalance(artisanWallet.getBalance().subtract(netRefundAmount));
        walletRepository.save(artisanWallet);
        
        log.info("Deducted {} VND from artisan wallet", netRefundAmount);
        
        // 6. Update CustomOrder with cancellation details
        order.setCancellationReason(reason);
        order.setCancelledBy(initiator);
        order.setCancelledAt(LocalDateTime.now());
        order.setStatus(initiator == CancellationInitiator.CUSTOMER 
            ? CustomOrderStatus.CANCELLED_BY_CUSTOMER 
            : CustomOrderStatus.CANCELLED_BY_ARTISAN);
        customOrderRepository.save(order);
        
        log.info("Updated order status to {}", order.getStatus());
        
        // 7. Create RefundTransaction record
        RefundTransaction refundTransaction = new RefundTransaction();
        refundTransaction.setCustomOrder(order);
        refundTransaction.setRefundSource(RefundSource.CANCELLATION);
        refundTransaction.setCancelledBy(initiator);
        refundTransaction.setAmount(grossRefundAmount);
        refundTransaction.setPlatformCommissionAmount(platformCommission);
        refundTransaction.setNetRefundAmount(netRefundAmount);
        refundTransaction.setFromWallet(artisanWallet);
        refundTransaction.setStatus(RefundStatus.PENDING);
        
        // Store calculation details as JSON
        String calculationDetails = buildCalculationDetailsJson(stageRefunds);
        refundTransaction.setCalculationDetails(calculationDetails);
        
        refundTransaction = refundTransactionRepository.save(refundTransaction);
        
        log.info("Created refund transaction: {}", refundTransaction.getRefundTransactionId());
        
        // 8. Process VNPay refund (subtask 4.5)
        try {
            processVNPayRefund(refundTransaction, order);
        } catch (Exception e) {
            log.error("VNPay refund failed: {}", e.getMessage(), e);
            refundTransaction.setStatus(RefundStatus.FAILED);
            refundTransaction.setFailureReason(e.getMessage());
            refundTransactionRepository.save(refundTransaction);
            throw new RuntimeException("Hoàn tiền VNPay thất bại: " + e.getMessage(), e);
        }
        
        // 9. Send notifications
        sendCancellationNotifications(order, initiator, reason, grossRefundAmount, platformCommission, netRefundAmount);
        
        log.info("Cancellation completed successfully for order {}", customOrderId);
        return refundTransaction;
    }
    
    @Override
    public CancellationEstimate calculateRefundEstimate(UUID customOrderId, CancellationInitiator initiator) {
        return refundCalculationService.calculateRefundEstimate(customOrderId, initiator);
    }
    
    @Override
    public boolean canCancelOrder(UUID customOrderId, CancellationInitiator initiator) {
        try {
            CustomOrder order = customOrderRepository.findById(customOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
            
            // Check if already cancelled
            if (order.getStatus().name().startsWith("CANCELLED")) {
                return false;
            }
            
            // Check if at least one paid stage exists
            boolean hasPaidStage = order.getStages().stream()
                .anyMatch(CustomOrderStage::getIsPaid);
            
            return hasPaidStage;
        } catch (Exception e) {
            log.error("Error checking if order can be cancelled: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate cancellation request
     * Requirements: 2.1, 3.1, 10.1
     * Subtask 4.2 & 9.3
     */
    private void validateCancellation(
        CustomOrder order,
        UUID initiatedBy,
        CancellationInitiator initiator,
        String reason
    ) {
        // Validate initiator is order participant (Requirement 10.1)
        UUID customerId = order.getRequest().getCustomer().getAccountId();
        UUID artisanId = order.getArtisan().getAccount().getAccountId();
        
        if (initiator == CancellationInitiator.CUSTOMER && !initiatedBy.equals(customerId)) {
            throw new CancellationException(
                "Chỉ khách hàng của đơn hàng mới có thể hủy",
                CancellationException.UNAUTHORIZED
            );
        }
        
        if (initiator == CancellationInitiator.ARTISAN && !initiatedBy.equals(artisanId)) {
            throw new CancellationException(
                "Chỉ nghệ nhân của đơn hàng mới có thể hủy",
                CancellationException.UNAUTHORIZED
            );
        }
        
        // Validate reason minimum 20 characters for Artisan cancellation (Requirement 3.1)
        if (initiator == CancellationInitiator.ARTISAN) {
            if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
                throw new CancellationException(
                    String.format("Lý do hủy phải có ít nhất %d ký tự", MIN_REASON_LENGTH),
                    CancellationException.INVALID_REASON
                );
            }
        }
        
        // Check if order is already cancelled (Requirement 10.1)
        if (order.getStatus().name().startsWith("CANCELLED")) {
            throw new CancellationException(
                "Đơn hàng đã bị hủy trước đó",
                CancellationException.ALREADY_CANCELLED
            );
        }
        
        // Validate order status allows cancellation
        if (order.getStatus() == CustomOrderStatus.COMPLETED) {
            throw new CancellationException(
                "Không thể hủy đơn hàng đã hoàn thành",
                CancellationException.INVALID_ORDER_STATUS
            );
        }
        
        // Validate at least one paid stage exists (Requirement 2.1)
        boolean hasPaidStage = order.getStages().stream()
            .anyMatch(CustomOrderStage::getIsPaid);
        
        if (!hasPaidStage) {
            throw new CancellationException(
                "Không có stage nào đã thanh toán để hoàn tiền",
                CancellationException.NO_PAID_STAGES
            );
        }
        
        log.info("Cancellation validation passed for order {}", order.getCustomOrderId());
    }
    
    /**
     * Process VNPay refund API call
     * Requirements: 2.6
     * Subtask 4.5
     */
    private void processVNPayRefund(RefundTransaction refundTransaction, CustomOrder order) {
        log.info("Processing VNPay refund for transaction {}", 
            refundTransaction.getRefundTransactionId());
        
        // Get all paid stages
        List<CustomOrderStage> paidStages = order.getStages().stream()
            .filter(CustomOrderStage::getIsPaid)
            .toList();
        
        if (paidStages.isEmpty()) {
            log.warn("No paid stages found for order {}", order.getCustomOrderId());
            refundTransaction.setStatus(RefundStatus.FAILED);
            refundTransaction.setFailureReason("Không có stage nào đã thanh toán");
            refundTransactionRepository.save(refundTransaction);
            throw new RefundProcessingException("Không có stage nào đã thanh toán", 
                RefundProcessingException.PAYMENT_NOT_FOUND);
        }
        
        // Calculate total refund amount and proportional amounts per stage
        BigDecimal totalRefundAmount = refundTransaction.getNetRefundAmount();
        BigDecimal totalPaidAmount = paidStages.stream()
            .map(CustomOrderStage::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Total paid amount: {}, Total refund amount: {}", totalPaidAmount, totalRefundAmount);
        
        List<String> vnpayRefundIds = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        // Process refund for each paid stage
        for (CustomOrderStage stage : paidStages) {
            try {
                // Find the successful payment for this stage
                StagePayment stagePayment = stagePaymentRepository
                    .findFirstByStage_StageIdAndStatusOrderByPaidAtDesc(
                        stage.getStageId(), 
                        PaymentStatus.SUCCESS
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thanh toán thành công cho stage " + stage.getName()
                    ));
                
                // Calculate proportional refund for this stage
                BigDecimal stageRefundAmount = calculateProportionalRefund(
                    stage.getAmount(),
                    totalRefundAmount,
                    totalPaidAmount
                );
                
                log.info("Processing refund for stage {}: {} VND (from original {})", 
                    stage.getName(), stageRefundAmount, stage.getAmount());
                
                // Validate payment has paidAt date
                if (stagePayment.getPaidAt() == null) {
                    String errorMsg = String.format("Stage %s không có ngày thanh toán", stage.getName());
                    log.error(errorMsg);
                    failureReasons.add(errorMsg);
                    failCount++;
                    continue;
                }
                
                // Convert payment date to VNPay format
                String originalTransactionDate = vnPayUtil.formatVNPayDate(stagePayment.getPaidAt());
                
                // Call VNPay refund API with retry logic
                VNPayRefundResponse vnpayResponse = callVNPayRefundWithRetry(
                    stagePayment.getTransactionId(),
                    originalTransactionDate,
                    stageRefundAmount,
                    "Hoàn tiền hủy đơn #" + order.getCustomOrderId()
                );
                
                // Check VNPay response
                if (!VNPayErrorMapper.isSuccess(vnpayResponse.getResponseCode())) {
                    String errorMsg = String.format("VNPay từ chối hoàn tiền stage %s. Mã lỗi: %s - %s",
                        stage.getName(), vnpayResponse.getResponseCode(), vnpayResponse.getMessage());
                    log.error(errorMsg);
                    failureReasons.add(errorMsg);
                    failCount++;
                    continue;
                }
                
                // Store VNPay refund ID
                vnpayRefundIds.add(vnpayResponse.getVnpayRefundId());
                successCount++;
                
                // Store the first refund ID as primary
                if (refundTransaction.getVnpayRefundId() == null) {
                    refundTransaction.setVnpayRefundId(vnpayResponse.getVnpayRefundId());
                    refundTransaction.setVnpayTransactionNo(vnpayResponse.getVnpayTransactionNo());
                    refundTransaction.setOriginalPaymentId(stagePayment.getPaymentId());
                }
                
                log.info("Stage refund successful. VNPay Refund ID: {}", vnpayResponse.getVnpayRefundId());
                
            } catch (Exception e) {
                String errorMsg = String.format("Lỗi khi hoàn tiền stage %s: %s", 
                    stage.getName(), e.getMessage());
                log.error(errorMsg, e);
                failureReasons.add(errorMsg);
                failCount++;
            }
        }
        
        // Update refund transaction status based on results
        if (failCount == 0) {
            refundTransaction.setStatus(RefundStatus.PROCESSING);
            log.info("All stage refunds initiated successfully");
            
            // Send refund completion notification to customer (Requirement 8.3)
            UUID customerId = order.getRequest().getCustomer().getAccountId();
            notificationService.notifyCustomerOfRefundCompletion(
                customerId,
                order.getCustomOrderId(),
                totalRefundAmount,
                refundTransaction.getVnpayTransactionNo()
            );
        } else if (successCount > 0) {
            refundTransaction.setStatus(RefundStatus.PARTIALLY_REFUNDED);
            refundTransaction.setFailureReason(String.join("; ", failureReasons));
            log.warn("Partial refund: {} succeeded, {} failed", successCount, failCount);
        } else {
            refundTransaction.setStatus(RefundStatus.FAILED);
            refundTransaction.setFailureReason(String.join("; ", failureReasons));
            log.error("All stage refunds failed");
            throw new RefundProcessingException("Tất cả stage refunds thất bại: " + String.join("; ", failureReasons),
                RefundProcessingException.PARTIAL_REFUND_FAILURE);
        }
        
        refundTransactionRepository.save(refundTransaction);
        
        log.info("VNPay refund processing completed. Success: {}, Failed: {}", successCount, failCount);
    }
    
    /**
     * Call VNPay refund API with retry logic
     * Requirements: 2.6
     */
    private VNPayRefundResponse callVNPayRefundWithRetry(
        String transactionId,
        String originalTransactionDate,
        BigDecimal amount,
        String reason
    ) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.info("VNPay refund attempt {} of {}", attempt, MAX_RETRY_ATTEMPTS);
                return vnPayUtil.createRefundRequest(transactionId, originalTransactionDate, amount, reason);
            } catch (org.example.catholicsouvenircustomorder.exception.VNPayTimeoutException e) {
                log.warn("VNPay timeout on attempt {}: {}", attempt, e.getMessage());
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    Thread.sleep(2000 * attempt); // Exponential backoff
                }
            } catch (org.example.catholicsouvenircustomorder.exception.VNPayNetworkException e) {
                log.warn("VNPay network error on attempt {}: {}", attempt, e.getMessage());
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    Thread.sleep(2000 * attempt); // Exponential backoff
                }
            } catch (Exception e) {
                // Non-retryable error
                log.error("VNPay refund failed with non-retryable error: {}", e.getMessage());
                throw e;
            }
        }
        
        // All retries exhausted
        log.error("VNPay refund failed after {} attempts", MAX_RETRY_ATTEMPTS);
        throw lastException;
    }
    
    /**
     * Calculate proportional refund amount for a stage
     */
    private BigDecimal calculateProportionalRefund(
        BigDecimal stageAmount,
        BigDecimal totalRefundAmount,
        BigDecimal totalPaidAmount
    ) {
        if (totalPaidAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate proportion: (stageAmount / totalPaidAmount) * totalRefundAmount
        BigDecimal proportion = stageAmount.divide(totalPaidAmount, 10, RoundingMode.HALF_UP);
        BigDecimal refundAmount = proportion.multiply(totalRefundAmount);
        
        // Round to 2 decimal places
        return refundAmount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Build JSON string for calculation details
     */
    private String buildCalculationDetailsJson(List<StageRefundCalculation> stageRefunds) {
        StringBuilder json = new StringBuilder("[");
        
        for (int i = 0; i < stageRefunds.size(); i++) {
            StageRefundCalculation calc = stageRefunds.get(i);
            if (i > 0) json.append(",");
            
            json.append("{")
                .append("\"stageId\":\"").append(calc.getStageId()).append("\",")
                .append("\"stageName\":\"").append(calc.getStageName()).append("\",")
                .append("\"paidAmount\":").append(calc.getPaidAmount()).append(",")
                .append("\"refundPercentage\":").append(calc.getRefundPercentage()).append(",")
                .append("\"grossRefund\":").append(calc.getGrossRefund()).append(",")
                .append("\"platformCommission\":").append(calc.getPlatformCommission()).append(",")
                .append("\"netRefund\":").append(calc.getNetRefund())
                .append("}");
        }
        
        json.append("]");
        return json.toString();
    }
    
    /**
     * Send notifications for cancellation
     * Requirements: 8.1, 8.2, 8.3
     */
    private void sendCancellationNotifications(
        CustomOrder order,
        CancellationInitiator initiator,
        String reason,
        BigDecimal grossRefundAmount,
        BigDecimal platformCommission,
        BigDecimal netRefundAmount
    ) {
        UUID customerId = order.getRequest().getCustomer().getAccountId();
        UUID artisanId = order.getArtisan().getAccount().getAccountId();
        String customerName = order.getRequest().getCustomer().getFullName();
        String artisanName = order.getArtisan().getAccount().getFullName();
        
        if (initiator == CancellationInitiator.CUSTOMER) {
            // Notify Artisan with gross refund, platform commission, and net refund
            notificationService.notifyArtisanOfCustomerCancellation(
                artisanId,
                order.getCustomOrderId(),
                customerName,
                reason,
                grossRefundAmount,
                platformCommission,
                netRefundAmount
            );
        } else {
            // Notify Customer with net refund amount (after platform commission deduction)
            notificationService.notifyCustomerOfArtisanCancellation(
                customerId,
                order.getCustomOrderId(),
                artisanName,
                reason,
                netRefundAmount
            );
        }
        
        log.info("Sent cancellation notifications for order {}", order.getCustomOrderId());
    }
}
