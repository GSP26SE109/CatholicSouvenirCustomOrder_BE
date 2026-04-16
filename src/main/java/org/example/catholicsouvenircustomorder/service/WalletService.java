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
     * Process payment and distribute money to multiple artisans
     * (90% to each artisan based on their order total, 10% platform fee)
     */
    void processPaymentDistribution(Payment payment, Account platformAdmin);
    
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
