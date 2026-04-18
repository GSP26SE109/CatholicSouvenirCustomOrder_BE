package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.response.Complaint.RefundTransactionResponse;
import org.example.catholicsouvenircustomorder.exception.InsufficientBalanceException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.RefundTransactionRepository;
import org.example.catholicsouvenircustomorder.repository.WalletRepository;
import org.example.catholicsouvenircustomorder.repository.WalletTransactionRepository;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.RefundService;
import org.example.catholicsouvenircustomorder.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of RefundService
 * Handles refund processing for approved complaints
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 11.4, 11.5
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
    
    /**
     * Process refund for approved complaint
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 11.4, 11.5
     */
    @Override
    @Transactional
    public RefundTransaction processRefund(Complaint complaint, BigDecimal amount) {
        log.info("Starting refund process for complaint: {}, amount: {}", 
                complaint.getComplaintId(), amount);
        
        // 1. Get wallets
        Wallet artisanWallet = walletService.getOrCreateWallet(complaint.getArtisan().getAccount());
        Wallet customerWallet = walletService.getOrCreateWallet(complaint.getCustomer());
        
        log.info("Artisan wallet balance: {}, Customer wallet balance: {}", 
                artisanWallet.getBalance(), customerWallet.getBalance());
        
        // 2. Check artisan wallet balance
        if (artisanWallet.getBalance().compareTo(amount) < 0) {
            log.error("Insufficient balance in artisan wallet. Required: {}, Available: {}", 
                    amount, artisanWallet.getBalance());
            
            // Create failed refund transaction
            RefundTransaction failedTransaction = new RefundTransaction();
            failedTransaction.setComplaint(complaint);
            failedTransaction.setAmount(amount);
            failedTransaction.setFromWallet(artisanWallet);
            failedTransaction.setToWallet(customerWallet);
            failedTransaction.setStatus(RefundStatus.FAILED);
            failedTransaction.setFailureReason("Insufficient balance in artisan wallet. Required: " 
                    + amount + ", Available: " + artisanWallet.getBalance());
            
            refundTransactionRepository.save(failedTransaction);
            
            // Send notification to admin
            try {
                Account platformAdmin = walletService.getPlatformAdminAccount();
                notificationService.sendNotification(
                    platformAdmin.getAccountId(),
                    NotificationType.REFUND_FAILED,
                    "Refund Failed - Insufficient Balance",
                    String.format("Refund for complaint #%s failed. Artisan %s does not have sufficient balance. Required: %s, Available: %s",
                            complaint.getComplaintId(),
                            complaint.getArtisan().getAccount().getFullName(),
                            amount,
                            artisanWallet.getBalance()),
                    complaint.getComplaintId()
                );
            } catch (Exception e) {
                log.error("Failed to send notification to admin: {}", e.getMessage());
            }
            
            throw new InsufficientBalanceException("Artisan wallet does not have sufficient balance for refund");
        }
        
        // 3. Create refund transaction with PENDING status
        RefundTransaction refundTransaction = new RefundTransaction();
        refundTransaction.setComplaint(complaint);
        refundTransaction.setAmount(amount);
        refundTransaction.setFromWallet(artisanWallet);
        refundTransaction.setToWallet(customerWallet);
        refundTransaction.setStatus(RefundStatus.PENDING);
        
        refundTransaction = refundTransactionRepository.save(refundTransaction);
        log.info("Created refund transaction: {}", refundTransaction.getRefundTransactionId());
        
        // 4. Create debit transaction for artisan (negative amount)
        BigDecimal artisanBalanceBefore = artisanWallet.getBalance();
        BigDecimal artisanBalanceAfter = artisanBalanceBefore.subtract(amount);
        
        WalletTransaction debitTx = new WalletTransaction();
        debitTx.setWallet(artisanWallet);
        debitTx.setType(WalletTransactionType.REFUND_DEBIT);
        debitTx.setAmount(amount.negate()); // Negative amount for debit
        debitTx.setBalanceBefore(artisanBalanceBefore);
        debitTx.setBalanceAfter(artisanBalanceAfter);
        debitTx.setRelatedEntityType(RelatedEntityType.COMPLAINT);
        debitTx.setRelatedEntityId(complaint.getComplaintId());
        debitTx.setDescription("Refund debit for complaint #" + complaint.getComplaintId());
        
        debitTx = walletTransactionRepository.save(debitTx);
        log.info("Created debit transaction: {}, Artisan balance: {} -> {}", 
                debitTx.getTransactionId(), artisanBalanceBefore, artisanBalanceAfter);
        
        // 5. Update artisan wallet balance
        artisanWallet.setBalance(artisanBalanceAfter);
        walletRepository.save(artisanWallet);
        
        // 6. Create credit transaction for customer (positive amount)
        BigDecimal customerBalanceBefore = customerWallet.getBalance();
        BigDecimal customerBalanceAfter = customerBalanceBefore.add(amount);
        
        WalletTransaction creditTx = new WalletTransaction();
        creditTx.setWallet(customerWallet);
        creditTx.setType(WalletTransactionType.REFUND_CREDIT);
        creditTx.setAmount(amount); // Positive amount for credit
        creditTx.setBalanceBefore(customerBalanceBefore);
        creditTx.setBalanceAfter(customerBalanceAfter);
        creditTx.setRelatedEntityType(RelatedEntityType.COMPLAINT);
        creditTx.setRelatedEntityId(complaint.getComplaintId());
        creditTx.setDescription("Refund credit for complaint #" + complaint.getComplaintId());
        
        creditTx = walletTransactionRepository.save(creditTx);
        log.info("Created credit transaction: {}, Customer balance: {} -> {}", 
                creditTx.getTransactionId(), customerBalanceBefore, customerBalanceAfter);
        
        // 7. Update customer wallet balance
        customerWallet.setBalance(customerBalanceAfter);
        walletRepository.save(customerWallet);
        
        // 8. Update refund transaction with transaction references and COMPLETED status
        refundTransaction.setDebitTransaction(debitTx);
        refundTransaction.setCreditTransaction(creditTx);
        refundTransaction.setStatus(RefundStatus.COMPLETED);
        refundTransaction.setCompletedAt(LocalDateTime.now());
        
        refundTransaction = refundTransactionRepository.save(refundTransaction);
        
        log.info("Refund process completed successfully for complaint: {}", complaint.getComplaintId());
        log.info("Final balances - Artisan: {}, Customer: {}", 
                artisanWallet.getBalance(), customerWallet.getBalance());
        
        return refundTransaction;
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
                .orElseThrow(() -> new ResourceNotFoundException("Refund transaction not found: " + refundTransactionId));
        
        // 2. Validate status is FAILED
        if (failedTransaction.getStatus() != RefundStatus.FAILED) {
            throw new IllegalStateException("Can only retry failed refund transactions. Current status: " 
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
                .orElseThrow(() -> new ResourceNotFoundException("Refund transaction not found: " + refundTransactionId));
        
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
     */
    private RefundTransactionResponse mapToResponse(RefundTransaction transaction) {
        return RefundTransactionResponse.builder()
                .refundTransactionId(transaction.getRefundTransactionId())
                .complaintId(transaction.getComplaint().getComplaintId())
                .amount(transaction.getAmount())
                .fromWalletId(transaction.getFromWallet().getWalletId())
                .fromWalletOwnerName(transaction.getFromWallet().getAccount().getFullName())
                .toWalletId(transaction.getToWallet().getWalletId())
                .toWalletOwnerName(transaction.getToWallet().getAccount().getFullName())
                .status(transaction.getStatus())
                .failureReason(transaction.getFailureReason())
                .debitTransactionId(transaction.getDebitTransaction() != null ? 
                        transaction.getDebitTransaction().getTransactionId() : null)
                .creditTransactionId(transaction.getCreditTransaction() != null ? 
                        transaction.getCreditTransaction().getTransactionId() : null)
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}
