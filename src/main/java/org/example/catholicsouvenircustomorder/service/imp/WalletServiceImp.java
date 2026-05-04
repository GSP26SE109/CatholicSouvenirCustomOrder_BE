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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImp implements WalletService {
    
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AccountRepository accountRepository;
    private final org.example.catholicsouvenircustomorder.repository.ArtisanRepository artisanRepository;
    private final org.example.catholicsouvenircustomorder.repository.OrderRepository orderRepository;
    private final org.example.catholicsouvenircustomorder.repository.PaymentRepository paymentRepository;
    private final org.example.catholicsouvenircustomorder.repository.StagePaymentRepository stagePaymentRepository;
    private final org.example.catholicsouvenircustomorder.service.NotificationService notificationService;
    
    // Platform fee rate: 10%
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10");
    
    @Override
    @Transactional
    public Wallet getOrCreateWallet(Account account) {
        // Validate that account is not a CUSTOMER
        if (account.getRole() != null && "CUSTOMER".equals(account.getRole().getName())) {
            throw new IllegalArgumentException(
                "Không hỗ trợ ví Customer. Hoàn tiền được xử lý qua VNPay."
            );
        }
        
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
    public void processPaymentDistribution(Payment payment, Account platformAdmin, 
                                          BigDecimal totalCommissionFee, BigDecimal commissionRate) {
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Payment must be SUCCESS to distribute money");
        }
        
        OrderGroup orderGroup = payment.getOrderGroup();
        if (orderGroup == null || orderGroup.getOrders() == null || orderGroup.getOrders().isEmpty()) {
            throw new IllegalStateException("Payment has no associated orders");
        }
        
        log.info("Starting payment distribution for OrderGroup: {}", orderGroup.getGroupId());
        log.info("Total payment amount: {}, Commission fee: {}, Commission rate: {}%", 
            payment.getAmount(), totalCommissionFee, commissionRate);
        
        // Extract order IDs to avoid navigating through lazy-loaded relationships
        List<UUID> orderIds = orderGroup.getOrders().stream()
                .map(Order::getOrderId)
                .collect(java.util.stream.Collectors.toList());
        
        log.info("Processing {} orders for distribution", orderIds.size());
        
        // Group orders by artisan using direct queries (no lazy loading)
        java.util.Map<UUID, java.util.List<UUID>> ordersByArtisanId = new java.util.HashMap<>();
        java.util.Map<UUID, Artisan> artisanCache = new java.util.HashMap<>();
        java.util.Map<UUID, BigDecimal> orderTotals = new java.util.HashMap<>();
        
        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        
        for (UUID orderId : orderIds) {
            // Find artisan for this order using queries
            Artisan artisan = findArtisanByOrderId(orderId);
            
            if (artisan == null) {
                log.error("Skipping order {} - no artisan found", orderId);
                continue;
            }
            
            UUID artisanId = artisan.getArtisanUuid();
            artisanCache.put(artisanId, artisan);
            
            // Get order total using query to avoid lazy loading
            BigDecimal orderTotal = getOrderTotal(orderId);
            orderTotals.put(orderId, orderTotal);
            totalOrderAmount = totalOrderAmount.add(orderTotal);
            
            // Group by artisan
            ordersByArtisanId.computeIfAbsent(artisanId, k -> new java.util.ArrayList<>()).add(orderId);
        }
        
        BigDecimal totalCommissionCollected = BigDecimal.ZERO;
        
        // Distribute to each artisan
        for (var entry : ordersByArtisanId.entrySet()) {
            UUID artisanId = entry.getKey();
            List<UUID> artisanOrderIds = entry.getValue();
            Artisan artisan = artisanCache.get(artisanId);
            
            // Calculate total amount for this artisan's orders
            BigDecimal artisanOrdersTotal = artisanOrderIds.stream()
                    .map(orderTotals::get)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate commission for this artisan proportionally
            BigDecimal artisanCommissionFee = BigDecimal.ZERO;
            if (totalCommissionFee != null && totalCommissionFee.compareTo(BigDecimal.ZERO) > 0 
                && totalOrderAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Commission is proportional to artisan's share of total orders
                artisanCommissionFee = totalCommissionFee
                    .multiply(artisanOrdersTotal)
                    .divide(totalOrderAmount, 2, RoundingMode.HALF_UP);
            }
            
            // Calculate final artisan amount (after commission)
            BigDecimal artisanNetAmount = artisanOrdersTotal.subtract(artisanCommissionFee);
            
            totalCommissionCollected = totalCommissionCollected.add(artisanCommissionFee);
            
            log.info("Artisan {}: OrdersTotal={}, CommissionFee={}, NetAmount={}", 
                    artisanId, artisanOrdersTotal, artisanCommissionFee, artisanNetAmount);
            
            // Deposit NET AMOUNT to artisan wallet (after commission)
            WalletTransaction walletTx = depositToWallet(artisan.getAccount(), artisanNetAmount, WalletTransactionType.DEPOSIT, 
                    payment, null,
                    String.format("Nạp tiền từ %d đơn hàng (OrderGroup #%s)", 
                            artisanOrderIds.size(), orderGroup.getGroupId()),
                    artisanCommissionFee, commissionRate);
            
            // Send notification to artisan about commission deduction
            if (artisanCommissionFee.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    // Use the first order ID as reference for the notification
                    UUID firstOrderId = artisanOrderIds.get(0);
                    notificationService.notifyArtisanCommissionDeducted(
                        artisanId,
                        firstOrderId,
                        artisanOrdersTotal, // original amount before commission
                        artisanCommissionFee,
                        artisanNetAmount,
                        walletTx.getTransactionId()
                    );
                } catch (Exception e) {
                    log.error("Failed to send commission notification to artisan {}: {}", 
                        artisanId, e.getMessage());
                    // Continue processing even if notification fails
                }
            }
        }
        
        // Collect total commission to admin wallet
        if (totalCommissionCollected.compareTo(BigDecimal.ZERO) > 0) {
            depositToWallet(platformAdmin, totalCommissionCollected, WalletTransactionType.PLATFORM_FEE, 
                    payment, null,
                    String.format("Phí sàn %.0f%% từ OrderGroup #%s", commissionRate, orderGroup.getGroupId()),
                    BigDecimal.ZERO, null);
            log.info("Total commission collected: {}", totalCommissionCollected);
        }
        
        log.info("Payment distribution completed for OrderGroup: {}", orderGroup.getGroupId());
    }
    
    /**
     * Helper method to find artisan from order using queries (no circular reference)
     */
    private Artisan findArtisanByOrderId(UUID orderId) {
        // Try to find artisan from product (order details)
        Optional<Artisan> artisanFromProduct = artisanRepository.findByOrderIdFromProduct(orderId);
        if (artisanFromProduct.isPresent()) {
            return artisanFromProduct.get();
        }
        
        // Try to find artisan from template (order template details)
        Optional<Artisan> artisanFromTemplate = artisanRepository.findByOrderIdFromTemplate(orderId);
        return artisanFromTemplate.orElse(null);
    }
    
    /**
     * Get order total using direct query to avoid lazy loading
     */
    private BigDecimal getOrderTotal(UUID orderId) {
        BigDecimal total = orderRepository.findTotalByOrderId(orderId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    @Override
    @Transactional
    public void processStagePaymentDistribution(StagePayment stagePayment, Artisan artisan, Account platformAdmin) {
        if (stagePayment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("StagePayment must be SUCCESS to distribute money");
        }
        
        if (artisan == null) {
            log.error("Cannot distribute stage payment - artisan is null");
            throw new IllegalStateException("Artisan not found for stage payment distribution");
        }
        
        BigDecimal totalAmount = stagePayment.getAmount();
        
        // Calculate platform fee (10%)
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate artisan amount (90%)
        BigDecimal artisanAmount = totalAmount.subtract(platformFee);
        
        log.info("Distributing stage payment {}: Total={}, PlatformFee={}, ArtisanAmount={}", 
                stagePayment.getPaymentId(), totalAmount, platformFee, artisanAmount);
        
        // Get stage ID safely without navigating through lazy-loaded relationships
        UUID stageId = null;
        try {
            if (stagePayment.getStage() != null) {
                stageId = stagePayment.getStage().getStageId();
            }
        } catch (Exception e) {
            log.warn("Could not get stageId: {}", e.getMessage());
        }
        
        String stageIdStr = stageId != null ? stageId.toString() : "unknown";
        
        // 1. Deposit to artisan wallet (90%)
        depositToWallet(artisan.getAccount(), artisanAmount, WalletTransactionType.DEPOSIT, null, stagePayment,
                "Nạp tiền từ custom order stage #" + stageIdStr, BigDecimal.ZERO, null);
        
        // 2. Collect platform fee to admin wallet (10%)
        depositToWallet(platformAdmin, platformFee, WalletTransactionType.PLATFORM_FEE, null, stagePayment,
                "Phí sàn 10% từ custom order stage #" + stageIdStr, BigDecimal.ZERO, null);
        
        log.info("Stage payment distribution completed for stage: {}", stageIdStr);
    }
    
    @Override
    @Transactional
    public WalletTransaction processStagePaymentDistributionWithCommission(StagePayment stagePayment, Artisan artisan, 
                                                             Account platformAdmin, BigDecimal commissionFee, 
                                                             BigDecimal commissionRate) {
        if (stagePayment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("StagePayment must be SUCCESS to distribute money");
        }
        
        if (artisan == null) {
            log.error("Cannot distribute stage payment - artisan is null");
            throw new IllegalStateException("Artisan not found for stage payment distribution");
        }
        
        BigDecimal totalAmount = stagePayment.getAmount();
        
        // Calculate platform fee (10%)
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate artisan amount before commission (90%)
        BigDecimal artisanAmountBeforeCommission = totalAmount.subtract(platformFee);
        
        // Calculate artisan net amount (after commission deduction)
        BigDecimal artisanNetAmount = artisanAmountBeforeCommission.subtract(commissionFee);
        
        // Calculate 70/30 split for partial withdrawal
        BigDecimal lockedPercentage = new BigDecimal("0.30");
        BigDecimal lockedAmount = artisanNetAmount.multiply(lockedPercentage)
                .setScale(2, RoundingMode.HALF_UP);
        
        log.info("Distributing stage payment {} with commission and 70/30 split: Total={}, PlatformFee={}, Commission={}, ArtisanNet={}, Locked30%={}", 
                stagePayment.getPaymentId(), totalAmount, platformFee, commissionFee, artisanNetAmount, lockedAmount);
        
        // Get stage ID safely
        UUID stageId = null;
        try {
            if (stagePayment.getStage() != null) {
                stageId = stagePayment.getStage().getStageId();
            }
        } catch (Exception e) {
            log.warn("Could not get stageId: {}", e.getMessage());
        }
        
        String stageIdStr = stageId != null ? stageId.toString() : "unknown";
        
        // 1. Deposit NET AMOUNT to artisan wallet (90% - commission) and lock 30%
        WalletTransaction artisanTransaction = depositToWalletWithLock(artisan.getAccount(), artisanNetAmount, lockedAmount, 
                WalletTransactionType.DEPOSIT, null, stagePayment,
                "Nạp tiền từ custom order stage #" + stageIdStr + " (70% available, 30% locked)", 
                commissionFee, commissionRate);
        
        // 2. Collect platform fee to admin wallet (10%)
        depositToWallet(platformAdmin, platformFee, WalletTransactionType.PLATFORM_FEE, null, stagePayment,
                "Phí sàn 10% từ custom order stage #" + stageIdStr, BigDecimal.ZERO, null);
        
        log.info("Stage payment distribution with commission and 70/30 split completed for stage: {}", stageIdStr);
        
        return artisanTransaction;
    }
    
    /**
     * Internal method to deposit money to wallet
     */
    private WalletTransaction depositToWallet(Account account, BigDecimal amount, WalletTransactionType type,
                                  Payment payment, StagePayment stagePayment, String description,
                                  BigDecimal commissionFee, BigDecimal commissionRate) {
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
        
        // Apply commission if provided
        if (commissionFee != null && commissionFee.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setCommissionFee(commissionFee);
            transaction.setCommissionRate(commissionRate);
            log.info("Commission applied to transaction: fee={}, rate={}%", commissionFee, commissionRate);
        }
        
        transaction = walletTransactionRepository.save(transaction);
        
        log.info("Deposited {} to wallet {}: {} -> {}", amount, wallet.getWalletId(), 
                balanceBefore, balanceAfter);
        
        return transaction;
    }
    
    /**
     * Internal method to deposit money to wallet with locked balance (70/30 split)
     */
    private WalletTransaction depositToWalletWithLock(Account account, BigDecimal amount, BigDecimal lockedAmount,
                                  WalletTransactionType type, Payment payment, StagePayment stagePayment, 
                                  String description, BigDecimal commissionFee, BigDecimal commissionRate) {
        Wallet wallet = getOrCreateWallet(account);
        
        BigDecimal balanceBefore = wallet.getBalance();
        // Handle null lockedBalance for old wallets
        BigDecimal lockedBalanceBefore = wallet.getLockedBalance() != null ? wallet.getLockedBalance() : BigDecimal.ZERO;
        
        // Add full amount to balance
        BigDecimal balanceAfter = balanceBefore.add(amount);
        
        // Add locked amount to lockedBalance
        BigDecimal lockedBalanceAfter = lockedBalanceBefore.add(lockedAmount);
        
        // Update wallet
        wallet.setBalance(balanceAfter);
        wallet.setLockedBalance(lockedBalanceAfter);
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
        
        // Apply commission if provided
        if (commissionFee != null && commissionFee.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setCommissionFee(commissionFee);
            transaction.setCommissionRate(commissionRate);
            log.info("Commission applied to transaction: fee={}, rate={}%", commissionFee, commissionRate);
        }
        
        transaction = walletTransactionRepository.save(transaction);
        
        log.info("Deposited {} to wallet {} with {} locked: balance {} -> {}, lockedBalance {} -> {}", 
                amount, wallet.getWalletId(), lockedAmount, balanceBefore, balanceAfter, 
                lockedBalanceBefore, lockedBalanceAfter);
        
        return transaction;
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