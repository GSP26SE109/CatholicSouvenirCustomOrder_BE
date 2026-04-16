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
    public void processPaymentDistribution(Payment payment, Account platformAdmin) {
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Payment must be SUCCESS to distribute money");
        }
        
        OrderGroup orderGroup = payment.getOrderGroup();
        if (orderGroup == null || orderGroup.getOrders() == null || orderGroup.getOrders().isEmpty()) {
            throw new IllegalStateException("Payment has no associated orders");
        }
        
        log.info("Starting payment distribution for OrderGroup: {}", orderGroup.getGroupId());
        log.info("Total payment amount: {}", payment.getAmount());
        
        // Group orders by artisan and calculate total per artisan
        var ordersByArtisan = orderGroup.getOrders().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        this::findArtisanByOrder,
                        java.util.stream.Collectors.toList()
                ));
        
        BigDecimal totalPlatformFee = BigDecimal.ZERO;
        
        // Distribute to each artisan
        for (var entry : ordersByArtisan.entrySet()) {
            Artisan artisan = entry.getKey();
            List<Order> artisanOrders = entry.getValue();
            
            if (artisan == null) {
                log.error("Skipping orders with no artisan: {}", artisanOrders.size());
                continue;
            }
            
            // Calculate total amount for this artisan's orders
            BigDecimal artisanOrdersTotal = artisanOrders.stream()
                    .map(Order::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate platform fee (10% of artisan's orders)
            BigDecimal platformFee = artisanOrdersTotal.multiply(PLATFORM_FEE_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Calculate artisan amount (90% of artisan's orders)
            BigDecimal artisanAmount = artisanOrdersTotal.subtract(platformFee);
            
            totalPlatformFee = totalPlatformFee.add(platformFee);
            
            log.info("Artisan {}: OrdersTotal={}, PlatformFee={}, ArtisanAmount={}", 
                    artisan.getArtisanUuid(), artisanOrdersTotal, platformFee, artisanAmount);
            
            // Deposit to artisan wallet (90%)
            String orderIds = artisanOrders.stream()
                    .map(o -> o.getOrderId().toString())
                    .collect(java.util.stream.Collectors.joining(", "));
            
            depositToWallet(artisan.getAccount(), artisanAmount, WalletTransactionType.DEPOSIT, payment, null,
                    String.format("Nạp tiền từ %d đơn hàng (OrderGroup #%s)", 
                            artisanOrders.size(), orderGroup.getGroupId()));
        }
        
        // Collect total platform fee to admin wallet (10%)
        if (totalPlatformFee.compareTo(BigDecimal.ZERO) > 0) {
            depositToWallet(platformAdmin, totalPlatformFee, WalletTransactionType.PLATFORM_FEE, payment, null,
                    String.format("Phí sàn 10%% từ OrderGroup #%s", orderGroup.getGroupId()));
            log.info("Total platform fee collected: {}", totalPlatformFee);
        }
        
        log.info("Payment distribution completed for OrderGroup: {}", orderGroup.getGroupId());
    }
    
    /**
     * Helper method to find artisan from order
     */
    private Artisan findArtisanByOrder(Order order) {
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            Product product = order.getOrderDetails().get(0).getProduct();
            if (product != null && product.getArtisan() != null) {
                return product.getArtisan();
            }
        }
        
        if (order.getTemplateDetails() != null && !order.getTemplateDetails().isEmpty()) {
            ProductTemplate template = order.getTemplateDetails().get(0).getTemplate();
            if (template != null && template.getArtisan() != null) {
                return template.getArtisan();
            }
        }
        
        return null;
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
