package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    
    /**
     * Get or create wallet for account
     */
    Wallet getOrCreateWallet(Account account);
    
    /**
     * Process payment and distribute money (90% to artisan, 10% platform fee)
     */
    void processPaymentDistribution(Payment payment, Artisan artisan, Account platformAdmin);
    
    /**
     * Process stage payment and distribute money
     */
    void processStagePaymentDistribution(StagePayment stagePayment, Artisan artisan, Account platformAdmin);
    
    /**
     * Get wallet balance
     */
    BigDecimal getBalance(UUID accountId);
    
    /**
     * Get transaction history
     */
    List<WalletTransaction> getTransactionHistory(UUID walletId);
    
    /**
     * Get platform admin account (for platform fee)
     */
    Account getPlatformAdminAccount();
}
