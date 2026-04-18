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
     * Now also handles commission deduction from artisan amounts
     * 
     * @param payment The payment to distribute
     * @param platformAdmin The platform admin account
     * @param totalCommissionFee Total commission fee to deduct from artisan payments
     * @param commissionRate Commission rate applied (for recording in transactions)
     */
    void processPaymentDistribution(Payment payment, Account platformAdmin, 
                                   BigDecimal totalCommissionFee, BigDecimal commissionRate);
    
    /**
     * Process stage payment and distribute money
     */
    void processStagePaymentDistribution(StagePayment stagePayment, Artisan artisan, Account platformAdmin);
    
    /**
     * Process stage payment and distribute money with commission deduction
     * 
     * @param stagePayment The stage payment to distribute
     * @param artisan The artisan receiving payment
     * @param platformAdmin The platform admin account
     * @param commissionFee Commission fee to deduct
     * @param commissionRate Commission rate applied
     * @return The wallet transaction created for the artisan
     */
    WalletTransaction processStagePaymentDistributionWithCommission(StagePayment stagePayment, Artisan artisan, 
                                                      Account platformAdmin, BigDecimal commissionFee, 
                                                      BigDecimal commissionRate);
    
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
