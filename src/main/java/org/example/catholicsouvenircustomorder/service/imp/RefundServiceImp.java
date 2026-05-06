package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.RefundTransactionResponse;
import org.example.catholicsouvenircustomorder.dto.response.VNPayRefundResponse;
import org.example.catholicsouvenircustomorder.exception.InsufficientBalanceException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.PaymentRepository;
import org.example.catholicsouvenircustomorder.repository.RefundTransactionRepository;
import org.example.catholicsouvenircustomorder.repository.StagePaymentRepository;
import org.example.catholicsouvenircustomorder.repository.WalletRepository;
import org.example.catholicsouvenircustomorder.repository.WalletTransactionRepository;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.RefundService;
import org.example.catholicsouvenircustomorder.service.WalletService;
import org.example.catholicsouvenircustomorder.util.VNPayErrorMapper;
import org.example.catholicsouvenircustomorder.util.VNPayUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of RefundService
 * Handles refund processing for approved complaints via VNPay
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 11.1, 11.2, 11.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImp implements RefundService {

    private final RefundTransactionRepository refundTransactionRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final VNPayUtil vnPayUtil;
    private final PaymentRepository paymentRepository;
    private final StagePaymentRepository stagePaymentRepository;

    /**
     * Process refund for approved complaint via VNPay
     * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 11.1, 11.2, 11.3
     */
    @Override
    @Transactional
    public RefundTransaction processRefund(Complaint complaint, BigDecimal amount) {
        log.info("Starting VNPay refund process for complaint: {}, amount: {}",
                complaint.getComplaintId(), amount);

        // 1. Get artisan wallet (Customer wallet removed - refund via VNPay)
        Wallet artisanWallet = walletService.getOrCreateWallet(complaint.getArtisan().getAccount());
        log.info("Artisan wallet balance: {}", artisanWallet.getBalance());

        // 2. Find original payment(s)
        List<Payment> originalPayments = findOriginalPayments(complaint);
        if (originalPayments.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán gốc cho khiếu nại này");
        }

        // 3. Create RefundTransaction with status PENDING
        RefundTransaction refundTransaction = new RefundTransaction();
        refundTransaction.setComplaint(complaint);
        refundTransaction.setAmount(amount);
        refundTransaction.setFromWallet(artisanWallet);
        refundTransaction.setStatus(RefundStatus.PENDING);
        refundTransaction = refundTransactionRepository.save(refundTransaction);

        log.info("Created refund transaction: {}", refundTransaction.getRefundTransactionId());

        // 4. Process VNPay refund based on order type
        try {
            if (complaint.getOrder() != null) {
                // Regular order: Single payment refund
                processRegularOrderRefund(refundTransaction, originalPayments.get(0), amount);
            } else if (complaint.getCustomOrder() != null) {
                // Custom order: Multiple stage payments refund
                processCustomOrderRefund(refundTransaction, originalPayments, amount);
            }
        } catch (Exception e) {
            log.error("VNPay refund failed: {}", e.getMessage(), e);

            // Mark refund as FAILED
            refundTransaction.setStatus(RefundStatus.FAILED);
            refundTransaction.setFailureReason(e.getMessage());
            refundTransactionRepository.save(refundTransaction);

            // Send notification to customer about failed refund
            try {
                notificationService.notifyCustomerRefundFailed(
                        complaint.getCustomer().getAccountId(),
                        complaint.getComplaintId(),
                        amount,
                        e.getMessage()
                );
            } catch (Exception notifEx) {
                log.error("Failed to send notification to customer: {}", notifEx.getMessage());
            }

            // Requirements: 12.4 - Send notification to admin when VNPay refund fails
            try {
                Account platformAdmin = walletService.getPlatformAdminAccount();
                String adminMessage = String.format(
                        "Hoàn tiền VNPay thất bại cho khiếu nại #%s. " +
                                "Khách hàng: %s, Số tiền: %,d VNĐ, Lỗi: %s. " +
                                "Vui lòng kiểm tra và xử lý thủ công nếu cần.",
                        complaint.getComplaintId(),
                        complaint.getCustomer().getFullName(),
                        amount.longValue(),
                        e.getMessage()
                );

                notificationService.sendNotification(
                        platformAdmin.getAccountId(),
                        NotificationType.REFUND_FAILED,
                        "Hoàn tiền VNPay thất bại - Cần xử lý",
                        adminMessage,
                        complaint.getComplaintId()
                );

                log.info("Sent VNPay refund failure notification to admin: {}", platformAdmin.getAccountId());
            } catch (Exception notifEx) {
                log.error("Failed to send notification to admin: {}", notifEx.getMessage());
            }

            // Requirements: 12.3 - Implement exception handling in processRefund()
            // Determine if this is a retryable error or permanent failure
            String errorMessage = "Hoàn tiền VNPay thất bại: " + e.getMessage();
            if (e instanceof org.example.catholicsouvenircustomorder.exception.VNPayTimeoutException ||
                    e instanceof org.example.catholicsouvenircustomorder.exception.VNPayNetworkException) {
                errorMessage += " (Có thể thử lại sau)";
            } else {
                errorMessage += " (Lỗi vĩnh viễn - cần xử lý thủ công)";
            }

            throw new RuntimeException(errorMessage, e);
        }

        // 5. Deduct from artisan wallet (even if VNPay is processing)
        deductFromArtisanWallet(refundTransaction, artisanWallet, amount);

        log.info("Refund process completed for complaint: {}", complaint.getComplaintId());
        return refundTransaction;
    }

    /**
     * Find original Payment or StagePayment entities for a complaint
     * Requirements: 11.1, 11.2
     */
    private List<Payment> findOriginalPayments(Complaint complaint) {
        List<Payment> payments = new ArrayList<>();

        if (complaint.getOrder() != null && complaint.getOrder().getOrderGroup() != null) {
            // Regular order - find payment by order group
            UUID orderGroupId = complaint.getOrder().getOrderGroup().getGroupId();
            List<Payment> groupPayments = paymentRepository.findByOrderGroup_GroupId(orderGroupId);

            // Find successful payment
            for (Payment payment : groupPayments) {
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    payments.add(payment);
                    log.info("Found original payment for regular order: {}", payment.getPaymentId());
                    break;
                }
            }
        } else if (complaint.getCustomOrder() != null) {
            // Custom order - find all successful stage payments
            CustomOrder customOrder = complaint.getCustomOrder();
            for (CustomOrderStage stage : customOrder.getStages()) {
                List<StagePayment> stagePayments = stagePaymentRepository
                        .findByStage_StageIdOrderByCreatedAtDesc(stage.getStageId());

                // Find successful payment for this stage
                for (StagePayment stagePayment : stagePayments) {
                    if (stagePayment.getStatus() == PaymentStatus.SUCCESS) {
                        // Create a Payment wrapper for StagePayment
                        Payment paymentWrapper = new Payment();
                        paymentWrapper.setPaymentId(stagePayment.getPaymentId());
                        paymentWrapper.setTransactionId(stagePayment.getTransactionId());
                        paymentWrapper.setAmount(stagePayment.getAmount());
                        paymentWrapper.setStatus(stagePayment.getStatus());
                        paymentWrapper.setPaidAt(stagePayment.getPaidAt()); // Include paidAt for refund
                        payments.add(paymentWrapper);

                        log.info("Found stage payment for custom order stage {}: {}",
                                stage.getStageOrder(), stagePayment.getPaymentId());
                        break;
                    }
                }
            }
        }

        return payments;
    }

    /**
     * Process refund for regular order (single payment)
     * Requirements: 3.1, 3.2, 3.3, 12.3 - Exception handling
     */
    private void processRegularOrderRefund(
            RefundTransaction refundTransaction,
            Payment originalPayment,
            BigDecimal amount
    ) throws Exception {
        log.info("Processing regular order refund via VNPay");
        log.info("Original payment ID: {}, Transaction ID: {}, Paid At: {}",
                originalPayment.getPaymentId(), originalPayment.getTransactionId(), originalPayment.getPaidAt());

        // Validate that payment has paidAt date
        if (originalPayment.getPaidAt() == null) {
            throw new Exception("Không thể hoàn tiền: Giao dịch gốc không có ngày thanh toán");
        }

        try {
            // Convert payment date to VNPay format
            String originalTransactionDate = vnPayUtil.formatVNPayDate(originalPayment.getPaidAt());
            
            // Call VNPay refund API with original transaction date
            VNPayRefundResponse vnpayResponse = vnPayUtil.createRefundRequest(
                    originalPayment.getTransactionId(),
                    originalTransactionDate,
                    amount,
                    "Hoàn tiền cho khiếu nại #" + refundTransaction.getComplaint().getComplaintId()
            );

            // Check if VNPay returned an error
            if (!VNPayErrorMapper.isSuccess(vnpayResponse.getResponseCode())) {
                String errorMessage = String.format("VNPay từ chối hoàn tiền. Mã lỗi: %s - %s",
                        vnpayResponse.getResponseCode(), vnpayResponse.getMessage());
                log.error(errorMessage);
                throw new Exception(errorMessage);
            }

            // Update RefundTransaction with VNPay info
            refundTransaction.setVnpayRefundId(vnpayResponse.getVnpayRefundId());
            refundTransaction.setVnpayTransactionNo(vnpayResponse.getVnpayTransactionNo());
            refundTransaction.setOriginalPaymentId(originalPayment.getPaymentId());
            refundTransaction.setStatus(RefundStatus.PROCESSING);
            refundTransactionRepository.save(refundTransaction);

            log.info("VNPay refund initiated successfully. Refund ID: {}, Transaction No: {}",
                    vnpayResponse.getVnpayRefundId(), vnpayResponse.getVnpayTransactionNo());

            // Send notification to customer
            try {
                notificationService.notifyCustomerRefundProcessing(
                        refundTransaction.getComplaint().getCustomer().getAccountId(),
                        refundTransaction.getComplaint().getComplaintId(),
                        amount
                );
            } catch (Exception e) {
                log.error("Failed to send notification to customer: {}", e.getMessage());
            }

        } catch (org.example.catholicsouvenircustomorder.exception.VNPayTimeoutException e) {
            // VNPay timeout (retryable)
            log.error("VNPay timeout during refund request (retryable)", e);
            throw new Exception("VNPay timeout - có thể thử lại sau: " + e.getMessage(), e);
        } catch (org.example.catholicsouvenircustomorder.exception.VNPayNetworkException e) {
            // VNPay network error (retryable)
            log.error("VNPay network error during refund request (retryable)", e);
            throw new Exception("VNPay network error - có thể thử lại sau: " + e.getMessage(), e);
        } catch (org.example.catholicsouvenircustomorder.exception.VNPayException e) {
            // VNPay business logic error (non-retryable)
            String errorMessage = String.format("VNPay business error [%s]: %s",
                    e.getErrorCode() != null ? e.getErrorCode() : "unknown", e.getMessage());
            log.error(errorMessage, e);
            throw new Exception(errorMessage, e);
        } catch (Exception e) {
            // Other unexpected errors
            log.error("Unexpected error during regular order refund", e);
            throw new Exception("Lỗi không xác định khi hoàn tiền: " + e.getMessage(), e);
        }
    }

    /**
     * Process refund for custom order (multiple stage payments)
     * Requirements: 11.1, 11.2, 11.3
     */
    private void processCustomOrderRefund(
            RefundTransaction refundTransaction,
            List<Payment> stagePayments,
            BigDecimal totalRefundAmount
    ) throws Exception {
        log.info("Processing custom order refund via VNPay");
        log.info("Number of stage payments: {}, Total refund amount: {}",
                stagePayments.size(), totalRefundAmount);

        int successCount = 0;
        int failCount = 0;
        StringBuilder failureReasons = new StringBuilder();

        // Calculate total paid amount
        BigDecimal totalPaidAmount = stagePayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total paid amount from stages: {}", totalPaidAmount);

        // Refund each stage payment proportionally
        for (Payment stagePayment : stagePayments) {
            try {
                // Validate that payment has paidAt date
                if (stagePayment.getPaidAt() == null) {
                    failCount++;
                    String errorMsg = String.format("Stage %s không có ngày thanh toán",
                            stagePayment.getPaymentId());
                    failureReasons.append(errorMsg).append("; ");
                    log.error("Stage refund failed: {}", errorMsg);
                    continue;
                }

                // Calculate proportional refund amount for this stage
                BigDecimal stageRefundAmount = calculateProportionalRefund(
                        stagePayment.getAmount(),
                        totalRefundAmount,
                        totalPaidAmount
                );

                log.info("Refunding stage payment {}: {} VND (proportional from {}), Paid At: {}",
                        stagePayment.getPaymentId(), stageRefundAmount, stagePayment.getAmount(), stagePayment.getPaidAt());

                // Convert payment date to VNPay format
                String originalTransactionDate = vnPayUtil.formatVNPayDate(stagePayment.getPaidAt());

                // Call VNPay refund API for this stage
                VNPayRefundResponse vnpayResponse = vnPayUtil.createRefundRequest(
                        stagePayment.getTransactionId(),
                        originalTransactionDate,
                        stageRefundAmount,
                        "Hoàn tiền một phần cho khiếu nại #" + refundTransaction.getComplaint().getComplaintId()
                );

                // Check if VNPay returned an error for this stage
                if (!VNPayErrorMapper.isSuccess(vnpayResponse.getResponseCode())) {
                    String stageErrorMessage = String.format("VNPay từ chối hoàn tiền stage %s. Mã lỗi: %s - %s",
                            stagePayment.getPaymentId(), vnpayResponse.getResponseCode(), vnpayResponse.getMessage());
                    log.error(stageErrorMessage);
                    throw new Exception(stageErrorMessage);
                }

                successCount++;

                // Save the first refund ID as primary
                if (refundTransaction.getVnpayRefundId() == null) {
                    refundTransaction.setVnpayRefundId(vnpayResponse.getVnpayRefundId());
                    refundTransaction.setVnpayTransactionNo(vnpayResponse.getVnpayTransactionNo());
                    refundTransaction.setOriginalPaymentId(stagePayment.getPaymentId());
                }

                log.info("Stage refund successful. Refund ID: {}", vnpayResponse.getVnpayRefundId());

            } catch (org.example.catholicsouvenircustomorder.exception.VNPayTimeoutException e) {
                // VNPay timeout for this stage (retryable)
                failCount++;
                String errorMsg = String.format("Stage %s VNPay timeout (retryable): %s",
                        stagePayment.getPaymentId(), e.getMessage());
                failureReasons.append(errorMsg).append("; ");
                log.error("Stage refund failed: {}", errorMsg, e);
            } catch (org.example.catholicsouvenircustomorder.exception.VNPayNetworkException e) {
                // VNPay network error for this stage (retryable)
                failCount++;
                String errorMsg = String.format("Stage %s VNPay network error (retryable): %s",
                        stagePayment.getPaymentId(), e.getMessage());
                failureReasons.append(errorMsg).append("; ");
                log.error("Stage refund failed: {}", errorMsg, e);
            } catch (org.example.catholicsouvenircustomorder.exception.VNPayException e) {
                // VNPay business logic error for this stage
                failCount++;
                String errorMsg = String.format("Stage %s VNPay business error [%s]: %s",
                        stagePayment.getPaymentId(),
                        e.getErrorCode() != null ? e.getErrorCode() : "unknown",
                        e.getMessage());
                failureReasons.append(errorMsg).append("; ");
                log.error("Stage refund failed: {}", errorMsg, e);
            } catch (Exception e) {
                failCount++;
                String errorMsg = String.format("Stage %s unexpected error: %s",
                        stagePayment.getPaymentId(), e.getMessage());
                failureReasons.append(errorMsg).append("; ");
                log.error("Stage refund failed: {}", errorMsg, e);
            }
        }

        // Update status based on results
        if (failCount == 0) {
            refundTransaction.setStatus(RefundStatus.PROCESSING);
            log.info("All stage refunds initiated successfully");
        } else if (successCount > 0) {
            refundTransaction.setStatus(RefundStatus.PARTIALLY_REFUNDED);
            refundTransaction.setFailureReason(failureReasons.toString());
            log.warn("Partial refund: {} succeeded, {} failed", successCount, failCount);
        } else {
            refundTransaction.setStatus(RefundStatus.FAILED);
            refundTransaction.setFailureReason("Tất cả stage refunds thất bại: " + failureReasons);
            log.error("All stage refunds failed");
            throw new Exception("Tất cả stage refunds thất bại: " + failureReasons);
        }

        refundTransactionRepository.save(refundTransaction);

        // Send notification to customer
        try {
            if (failCount == 0) {
                // All refunds successful - send processing notification
                notificationService.notifyCustomerRefundProcessing(
                        refundTransaction.getComplaint().getCustomer().getAccountId(),
                        refundTransaction.getComplaint().getComplaintId(),
                        totalRefundAmount
                );
            } else if (successCount > 0) {
                // Partial refund - send custom notification
                String detailMessage = String.format(
                        "Một số giao dịch hoàn tiền thành công (%d/%d). Vui lòng liên hệ hỗ trợ nếu cần.",
                        successCount, stagePayments.size()
                );
                notificationService.sendNotification(
                        refundTransaction.getComplaint().getCustomer().getAccountId(),
                        NotificationType.REFUND_COMPLETED,
                        "Hoàn tiền một phần đang xử lý",
                        detailMessage,
                        refundTransaction.getComplaint().getComplaintId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to send notification to customer: {}", e.getMessage());
        }
    }

    /**
     * Calculate proportional refund amount for a stage payment
     * Requirements: 11.2
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
     * Deduct refund amount from artisan wallet
     * This method keeps the existing wallet deduction logic:
     * - Deducts the refund amount from artisan wallet
     * - Allows negative balance (no validation preventing it)
     * - Refunds commission back to artisan (negative commissionFee)
     * - Does NOT create creditTransaction for Customer (removed in refactor)
     *
     * Requirements: 3.2, 5.1, 5.2, 5.3, 5.4, 5.5
     */
    private void deductFromArtisanWallet(
            RefundTransaction refundTransaction,
            Wallet artisanWallet,
            BigDecimal amount
    ) {
        log.info("Deducting {} VND from artisan wallet (balance before: {})",
                amount, artisanWallet.getBalance());

        // Find original commission to refund back to artisan
        BigDecimal originalCommissionFee = findOriginalCommission(refundTransaction);

        // Create debit transaction
        BigDecimal balanceBefore = artisanWallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        // Requirement 5.2: Allow negative balance - no validation check here
        // The system will still process the refund even if artisan balance is insufficient

        WalletTransaction debitTx = new WalletTransaction();
        debitTx.setWallet(artisanWallet);
        debitTx.setType(WalletTransactionType.REFUND_DEBIT);
        debitTx.setAmount(amount.negate()); // Negative amount for debit
        debitTx.setBalanceBefore(balanceBefore);
        debitTx.setBalanceAfter(balanceAfter);
        debitTx.setRelatedEntityType(RelatedEntityType.COMPLAINT);
        debitTx.setRelatedEntityId(refundTransaction.getComplaint().getComplaintId());
        debitTx.setDescription("Hoàn tiền VNPay cho khiếu nại #" +
                refundTransaction.getComplaint().getComplaintId());

        // Requirement 5.3: Refund commission back to artisan (negative commission fee)
        // This effectively reduces the amount deducted from artisan
        if (originalCommissionFee.compareTo(BigDecimal.ZERO) > 0) {
            debitTx.setCommissionFee(originalCommissionFee.negate());
            log.info("Refunding commission to artisan: {} VND (reduces deduction)", originalCommissionFee);
        }

        debitTx = walletTransactionRepository.save(debitTx);
        log.info("Created debit transaction: {}, Artisan balance: {} -> {}",
                debitTx.getTransactionId(), balanceBefore, balanceAfter);

        // Requirement 5.2: Update artisan wallet balance (allow negative balance)
        artisanWallet.setBalance(balanceAfter);
        walletRepository.save(artisanWallet);

        // Link debit transaction to refund
        refundTransaction.setDebitTransaction(debitTx);
        refundTransactionRepository.save(refundTransaction);

        // Requirement 5.2: Log warning if balance goes negative
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Artisan wallet {} balance is now negative: {} VND (artisan owes platform)",
                    artisanWallet.getWalletId(), balanceAfter);
        }

        // Requirement 5.4: NO creditTransaction created for Customer
        // Customer receives refund directly via VNPay, not through wallet
    }

    /**
     * Find original commission fee from payment transactions
     * Requirements: 5.4
     */
    private BigDecimal findOriginalCommission(RefundTransaction refundTransaction) {
        BigDecimal originalCommissionFee = BigDecimal.ZERO;
        Complaint complaint = refundTransaction.getComplaint();
        Wallet artisanWallet = refundTransaction.getFromWallet();

        // Check if this is a regular order or custom order
        if (complaint.getOrder() != null && complaint.getOrder().getOrderGroup() != null) {
            // Regular order - find payment transaction
            List<WalletTransaction> artisanTransactions = walletTransactionRepository
                    .findByWallet_WalletIdOrderByCreatedAtDesc(artisanWallet.getWalletId());

            // Look for the payment transaction related to this order group
            UUID orderGroupId = complaint.getOrder().getOrderGroup().getGroupId();
            for (WalletTransaction tx : artisanTransactions) {
                if (tx.getPayment() != null &&
                        tx.getPayment().getOrderGroup() != null &&
                        tx.getPayment().getOrderGroup().getGroupId().equals(orderGroupId) &&
                        tx.getCommissionFee() != null) {
                    originalCommissionFee = tx.getCommissionFee();
                    log.info("Found original commission from payment: {} VND", originalCommissionFee);
                    break;
                }
            }
        } else if (complaint.getCustomOrder() != null) {
            // Custom order - sum up all commission fees from stage payments
            List<WalletTransaction> artisanTransactions = walletTransactionRepository
                    .findByWallet_WalletIdOrderByCreatedAtDesc(artisanWallet.getWalletId());

            UUID customOrderId = complaint.getCustomOrder().getCustomOrderId();
            for (WalletTransaction tx : artisanTransactions) {
                if (tx.getStagePayment() != null &&
                        tx.getStagePayment().getStage() != null &&
                        tx.getStagePayment().getStage().getCustomOrder() != null &&
                        tx.getStagePayment().getStage().getCustomOrder().getCustomOrderId().equals(customOrderId) &&
                        tx.getCommissionFee() != null) {
                    originalCommissionFee = originalCommissionFee.add(tx.getCommissionFee());
                    log.info("Found commission from stage payment: {} VND", tx.getCommissionFee());
                }
            }
            if (originalCommissionFee.compareTo(BigDecimal.ZERO) > 0) {
                log.info("Total commission from all stage payments: {} VND", originalCommissionFee);
            }
        }

        return originalCommissionFee;
    }

    /**
     * Retry failed refund (admin action)
     * Requirements: 11.5
     */
    @Override
    @Transactional
    public RefundTransaction retryRefund(UUID refundTransactionId, UUID adminId) {
        log.info("Admin {} attempting to retry refund transaction: {}", adminId, refundTransactionId);

        // 1. Validate refund transaction exists
        RefundTransaction failedTransaction = refundTransactionRepository.findById(refundTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch hoàn tiền không tồn tại: " + refundTransactionId));

        // 2. Validate status is FAILED
        if (failedTransaction.getStatus() != RefundStatus.FAILED) {
            throw new IllegalStateException("Chỉ có thể thử lại giao dịch hoàn tiền thất bại. Trạng thái hiện tại: "
                    + failedTransaction.getStatus());
        }

        log.info("Retrying failed refund for complaint: {}, amount: {}",
                failedTransaction.getComplaint().getComplaintId(), failedTransaction.getAmount());

        // 3. Call processRefund again with complaint and amount from failed transaction
        try {
            RefundTransaction newTransaction = processRefund(
                    failedTransaction.getComplaint(),
                    failedTransaction.getAmount()
            );

            log.info("Refund retry successful. New transaction: {}", newTransaction.getRefundTransactionId());
            return newTransaction;
        } catch (InsufficientBalanceException e) {
            log.error("Refund retry failed again due to insufficient balance");
            throw e;
        }
    }

    /**
     * Get refund transaction details
     * Requirements: 9.1
     */
    @Override
    public RefundTransactionResponse getRefundTransaction(UUID refundTransactionId) {
        RefundTransaction transaction = refundTransactionRepository.findById(refundTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch hoàn tiền không tồn tại: " + refundTransactionId));

        return mapToResponse(transaction);
    }

    /**
     * Get all refund transactions with optional status filter
     * Requirements: 9.1
     */
    @Override
    public Page<RefundTransactionResponse> getAllRefundTransactions(RefundStatus status, Pageable pageable) {
        Page<RefundTransaction> transactions;

        if (status != null) {
            transactions = refundTransactionRepository.findByStatus(status, pageable);
        } else {
            transactions = refundTransactionRepository.findAll(pageable);
        }

        return transactions.map(this::mapToResponse);
    }

    /**
     * Map RefundTransaction entity to response DTO
     * Includes VNPay refund fields
     */
    private RefundTransactionResponse mapToResponse(RefundTransaction transaction) {
        return RefundTransactionResponse.builder()
                .refundTransactionId(transaction.getRefundTransactionId())
                .complaintId(transaction.getComplaint().getComplaintId())
                .amount(transaction.getAmount())
                .fromWalletId(transaction.getFromWallet().getWalletId())
                .fromWalletOwnerName(transaction.getFromWallet().getAccount().getFullName())
                // VNPay refund fields
                .vnpayRefundId(transaction.getVnpayRefundId())
                .vnpayTransactionNo(transaction.getVnpayTransactionNo())
                .originalPaymentId(transaction.getOriginalPaymentId())
                .status(transaction.getStatus())
                .failureReason(transaction.getFailureReason())
                .debitTransactionId(transaction.getDebitTransaction() != null ?
                        transaction.getDebitTransaction().getTransactionId() : null)
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}