package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.WalletRepository;
import org.example.catholicsouvenircustomorder.repository.WalletTransactionRepository;
import org.example.catholicsouvenircustomorder.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImp implements WalletService {
    
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AccountRepository accountRepository;
    
    // Platform fee rate: 10%
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10");
    
    @Override
    @Transactional
    public Wallet getOrCreateWallet(Account account) {
        return walletRepository.findByAccount(account)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setAccount(account);
                    wallet.setBalance(BigDecimal.ZERO);
                    Wallet saved = walletRepository.save(wallet);
                    log.info("Created new wallet for account: {}", account.getAccountId());
                    return saved;
                });
    }
    
    @Override
    @Transactional
    public void processPaymentDistribution(Payment payment, Artisan artisan, Account platformAdmin) {
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Payment must be SUCCESS to distribute money");
        }
        
        BigDecimal totalAmount = payment.getAmount();
        
        // Calculate platform fee (10%)
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate artisan amount (90%)
        BigDecimal artisanAmount = totalAmount.subtract(platformFee);
        
        log.info("Distributing payment: Total={}, PlatformFee={}, ArtisanAmount={}", 
                totalAmount, platformFee, artisanAmount);
        
        // 1. Deposit to artisan wallet (90%)
        depositToWallet(artisan.getAccount(), artisanAmount, WalletTransactionType.DEPOSIT, payment, null,
                "Nạp tiền từ đơn hàng #" + payment.getOrder().getOrderId());
        
        // 2. Collect platform fee to admin wallet (10%)
        depositToWallet(platformAdmin, platformFee, WalletTransactionType.PLATFORM_FEE, payment, null,
                "Phí sàn 10% từ đơn hàng #" + payment.getOrder().getOrderId());
        
        log.info("Payment distribution completed for order: {}", payment.getOrder().getOrderId());
    }
    
    @Override
    @Transactional
    public void processStagePaymentDistribution(StagePayment stagePayment, Artisan artisan, Account platformAdmin) {
        if (stagePayment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("StagePayment must be SUCCESS to distribute money");
        }
        
        BigDecimal totalAmount = stagePayment.getAmount();
        
        // Calculate platform fee (10%)
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate artisan amount (90%)
        BigDecimal artisanAmount = totalAmount.subtract(platformFee);
        
        log.info("Distributing stage payment: Total={}, PlatformFee={}, ArtisanAmount={}", 
                totalAmount, platformFee, artisanAmount);
        
        // 1. Deposit to artisan wallet (90%)
        depositToWallet(artisan.getAccount(), artisanAmount, WalletTransactionType.DEPOSIT, null, stagePayment,
                "Nạp tiền từ custom order stage #" + stagePayment.getStage().getStageId());
        
        // 2. Collect platform fee to admin wallet (10%)
        depositToWallet(platformAdmin, platformFee, WalletTransactionType.PLATFORM_FEE, null, stagePayment,
                "Phí sàn 10% từ custom order stage #" + stagePayment.getStage().getStageId());
        
        log.info("Stage payment distribution completed for stage: {}", stagePayment.getStage().getStageId());
    }
    
    /**
     * Internal method to deposit money to wallet
     */
    private void depositToWallet(Account account, BigDecimal amount, WalletTransactionType type,
                                  Payment payment, StagePayment stagePayment, String description) {
        Wallet wallet = getOrCreateWallet(account);
        
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        
        // Update wallet balance
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);
        
        // Create transaction record
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setPayment(payment);
        transaction.setStagePayment(stagePayment);
        transaction.setDescription(description);
        
        walletTransactionRepository.save(transaction);
        
        log.info("Deposited {} to wallet {}: {} -> {}", amount, wallet.getWalletId(), 
                balanceBefore, balanceAfter);
    }
    
    @Override
    public BigDecimal getBalance(UUID accountId) {
        Wallet wallet = walletRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for account: " + accountId));
        return wallet.getBalance();
    }
    
    @Override
    public List<WalletTransaction> getTransactionHistory(UUID walletId) {
        return walletTransactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(walletId);
    }
    
    @Override
    public Account getPlatformAdminAccount() {
        // Find account with ADMIN role
        // You might want to have a specific platform admin account
        // For now, we'll find the first admin account
        return accountRepository.findAll().stream()
                .filter(account -> account.getRole() != null && 
                        "ADMIN".equals(account.getRole().getName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Platform admin account not found"));
    }
}
