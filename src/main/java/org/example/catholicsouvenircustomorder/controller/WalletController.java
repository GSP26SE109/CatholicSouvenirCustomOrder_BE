package org.example.catholicsouvenircustomorder.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.WalletResponse;
import org.example.catholicsouvenircustomorder.dto.response.WalletTransactionResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Wallet;
import org.example.catholicsouvenircustomorder.model.WalletTransaction;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.WalletRepository;
import org.example.catholicsouvenircustomorder.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    
    /**
     * Get wallet information
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN', 'ADMIN')")
    public ResponseEntity<BaseResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal UUID accountId) {
        
        log.info("GET /api/wallet - Account: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        Wallet wallet = walletService.getOrCreateWallet(account);
        
        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .accountId(account.getAccountId())
                .accountName(account.getFullName())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
        
        return ResponseEntity.ok(BaseResponse.<WalletResponse>builder()
                .code(200)
                .message("Lấy thông tin ví thành công")
                .data(response)
                .build());
    }
    
    /**
     * Get wallet balance
     */
    @GetMapping("/balance")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN', 'ADMIN')")
    public ResponseEntity<BaseResponse<BigDecimal>> getBalance(
            @AuthenticationPrincipal UUID accountId) {
        
        log.info("GET /api/wallet/balance - Account: {}", accountId);
        
        BigDecimal balance = walletService.getBalance(accountId);
        
        return ResponseEntity.ok(BaseResponse.<BigDecimal>builder()
                .code(200)
                .message("Lấy số dư ví thành công")
                .data(balance)
                .build());
    }
    
    /**
     * Get transaction history
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN', 'ADMIN')")
    public ResponseEntity<BaseResponse<List<WalletTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UUID accountId) {
        
        log.info("GET /api/wallet/transactions - Account: {}", accountId);
        
        Wallet wallet = walletRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        
        List<WalletTransaction> transactions = walletService.getTransactionHistory(wallet.getWalletId());
        
        List<WalletTransactionResponse> response = transactions.stream()
                .map(tx -> {
                    // Ensure commission_fee is never null (default to 0)
                    BigDecimal commissionFee = tx.getCommissionFee() != null ? 
                        tx.getCommissionFee() : BigDecimal.ZERO;
                    
                    return WalletTransactionResponse.builder()
                        .transactionId(tx.getTransactionId())
                        .walletId(tx.getWallet().getWalletId())
                        .type(tx.getType())
                        .amount(tx.getAmount())
                        .balanceBefore(tx.getBalanceBefore())
                        .balanceAfter(tx.getBalanceAfter())
                        .description(tx.getDescription())
                        .paymentId(tx.getPayment() != null ? tx.getPayment().getPaymentId() : null)
                        .stagePaymentId(tx.getStagePayment() != null ? tx.getStagePayment().getPaymentId() : null)
                        .commissionFee(commissionFee)
                        .commissionRate(tx.getCommissionRate())
                        .createdAt(tx.getCreatedAt())
                        .build();
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(BaseResponse.<List<WalletTransactionResponse>>builder()
                .code(200)
                .message("Lấy lịch sử giao dịch thành công")
                .data(response)
                .build());
    }
    
    /**
     * Get wallet by account ID (Admin only)
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<WalletResponse>> getWalletByAccountId(
            @PathVariable UUID accountId) {
        
        log.info("GET /api/wallet/account/{} - Admin request", accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        Wallet wallet = walletService.getOrCreateWallet(account);
        
        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .accountId(account.getAccountId())
                .accountName(account.getFullName())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
        
        return ResponseEntity.ok(BaseResponse.<WalletResponse>builder()
                .code(200)
                .message("Lấy thông tin ví thành công")
                .data(response)
                .build());
    }
}
