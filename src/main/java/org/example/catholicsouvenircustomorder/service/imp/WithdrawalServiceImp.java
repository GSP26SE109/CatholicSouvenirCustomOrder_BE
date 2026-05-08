package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.catholicsouvenircustomorder.dto.request.ApproveWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.RejectWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.WithdrawalFilterRequest;
import org.example.catholicsouvenircustomorder.dto.response.WithdrawalDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.WithdrawalResponse;
import org.example.catholicsouvenircustomorder.exception.*;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.WithdrawalService;
import org.example.catholicsouvenircustomorder.util.BankAccountUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalServiceImp implements WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final ArtisanRepository artisanRepository;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final NotificationService notificationService;
    private final BankAccountUtil bankAccountUtil;
    private final ComplaintRepository complaintRepository;
    private final org.example.catholicsouvenircustomorder.repository.OrderRepository orderRepository;

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    @Override
    @Transactional
    public WithdrawalResponse createWithdrawalRequest(UUID artisanId, CreateWithdrawalRequest request) {
        log.info("Creating withdrawal request for artisan: {}", artisanId);

        // 1. Validate artisan exists and has ARTISAN role
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Artisan not found"));

        Account account = artisan.getAccount();
        if (account == null || account.getRole() == null || !"ARTISAN".equals(account.getRole().getName())) {
            throw new BadRequestException("Account must have ARTISAN role");
        }

        // Validate account is verified
        if (!account.isVerified()) {
            throw new BadRequestException("Account must be verified to withdraw");
        }

        // Check if artisan has active complaints with withdrawal frozen
        boolean hasActiveComplaint = complaintRepository.hasActiveComplaintWithWithdrawalFrozen(artisanId);
        if (hasActiveComplaint) {
            throw new BadRequestException(
                    "Không thể rút tiền khi có khiếu nại đang được xử lý. " +
                            "Vui lòng đợi khiếu nại được giải quyết trước khi yêu cầu rút tiền."
            );
        }

        // 2. Validate amount (already validated by @Valid, but double-check)
        BigDecimal amount = request.getAmount();
        if (amount.compareTo(new BigDecimal("50000")) < 0) {
            throw new BadRequestException("Số tiền rút tối thiểu là 50,000 VND");
        }
        if (amount.compareTo(new BigDecimal("50000000")) > 0) {
            throw new BadRequestException("Số tiền rút tối đa là 50,000,000 VND");
        }

        // 3. Check wallet balance
        Wallet wallet = walletRepository.findByAccount(account)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        // Calculate available balance (total - locked)
        BigDecimal availableBalance = wallet.getAvailableBalance();
        
        // Calculate days until unlock (for better UX message)
        int daysUntilUnlock = calculateDaysUntilUnlock(artisanId);

        if (availableBalance.compareTo(amount) < 0) {
            String message = String.format(
                "Số dư khả dụng không đủ.\n\n" +
                "• Số dư khả dụng: %,d VND\n" +
                "• Số dư bị khóa: %,d VND%s\n" +
                "• Số tiền yêu cầu: %,d VND\n\n" +
                "💡 Số dư bị khóa là tiền từ đơn hàng trong thời gian bảo vệ khiếu nại (7 ngày). " +
                "Sau khi hết thời gian bảo vệ, số dư sẽ tự động mở khóa và bạn có thể rút.",
                availableBalance.longValue(),
                wallet.getLockedBalance().longValue(),
                daysUntilUnlock > 0 ? String.format(" (mở khóa sau ~%d ngày)", daysUntilUnlock) : "",
                amount.longValue()
            );
            throw new InsufficientBalanceException(message);
        }

        // 4. Check no pending withdrawal exists
        boolean hasPending = withdrawalRequestRepository.existsByArtisan_ArtisanUuidAndStatus(
                artisanId, WithdrawalStatus.PENDING
        );
        if (hasPending) {
            throw new PendingWithdrawalExistsException(
                    "Bạn đã có yêu cầu rút tiền đang chờ xử lý. Vui lòng đợi hoặc hủy yêu cầu hiện tại."
            );
        }

        // 5. Validate bank account info (already validated by @Valid)

        // 6. Create withdrawal request
        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setArtisan(artisan);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setBankName(request.getBankName());
        withdrawal.setBankAccountNumber(request.getBankAccountNumber());
        withdrawal.setBankAccountName(request.getBankAccountName());
        withdrawal.setReason(request.getReason()); // Set reason from DTO
        withdrawal.setCreatedAt(LocalDateTime.now());

        withdrawal = withdrawalRequestRepository.save(withdrawal);
        log.info("Withdrawal request created: {}", withdrawal.getWithdrawalId());

        // 7. Send notification to Admin with HIGH priority
        try {
            notificationService.notifyAdminOfWithdrawalRequest(
                    withdrawal.getWithdrawalId(),
                    account.getFullName(),
                    amount.longValue()
            );
        } catch (Exception e) {
            log.error("Failed to send notification to admin", e);
            // Don't fail the withdrawal creation if notification fails
        }

        // 8. Return response
        return mapToResponse(withdrawal);
    }

    @Override
    public Page<WithdrawalResponse> getWithdrawalsByArtisan(UUID artisanId, WithdrawalFilterRequest filter) {
        log.info("Getting withdrawals for artisan: {}", artisanId);

        // Validate artisan exists
        if (!artisanRepository.existsById(artisanId)) {
            throw new NotFoundException("Artisan not found");
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        Page<WithdrawalRequest> withdrawals;

        if (filter.getStatus() != null) {
            // Filter by status
            withdrawals = withdrawalRequestRepository.findByArtisan_ArtisanUuidOrderByCreatedAtDesc(
                    artisanId, pageable
            );
            // Apply status filter manually since we need to filter by artisan first
            withdrawals = withdrawals.map(w -> w.getStatus() == filter.getStatus() ? w : null)
                    .map(w -> w);
        } else {
            // No status filter
            withdrawals = withdrawalRequestRepository.findByArtisan_ArtisanUuidOrderByCreatedAtDesc(
                    artisanId, pageable
            );
        }

        return withdrawals.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void cancelWithdrawal(UUID artisanId, UUID withdrawalId) {
        log.info("Cancelling withdrawal: {} by artisan: {}", withdrawalId, artisanId);

        // 1. Find withdrawal
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new NotFoundException("Withdrawal request not found"));

        // 2. Validate withdrawal belongs to artisan
        if (!withdrawal.getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new UnauthorizedException("You are not authorized to cancel this withdrawal");
        }

        // 3. Validate status is PENDING
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new InvalidStatusException(
                    "Chỉ có thể hủy yêu cầu rút tiền đang chờ xử lý. Trạng thái hiện tại: " + withdrawal.getStatus()
            );
        }

        // 4. Update status to CANCELLED
        withdrawal.setStatus(WithdrawalStatus.CANCELLED);
        withdrawal.setCancelledAt(LocalDateTime.now());
        withdrawalRequestRepository.save(withdrawal);

        log.info("Withdrawal cancelled: {}", withdrawalId);

        // 5. Send notification to Admin
        try {
            notificationService.notifyAdminOfWithdrawalCancellation(
                    withdrawalId,
                    withdrawal.getArtisan().getAccount().getFullName(),
                    withdrawal.getAmount().longValue()
            );
        } catch (Exception e) {
            log.error("Failed to send notification to admin", e);
        }
    }

    @Override
    public Page<WithdrawalResponse> getAllWithdrawals(WithdrawalFilterRequest filter) {
        log.info("Getting all withdrawals with filter");

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());
        Page<WithdrawalRequest> withdrawals;

        // Apply filters based on what's provided
        boolean hasStatus = filter.getStatus() != null;
        boolean hasArtisanName = filter.getArtisanName() != null && !filter.getArtisanName().trim().isEmpty();
        boolean hasDateRange = filter.getFromDate() != null && filter.getToDate() != null;

        if (hasStatus && hasArtisanName) {
            // Filter by both status and artisan name
            withdrawals = withdrawalRequestRepository
                    .findByStatusAndArtisan_Account_FullNameContainingIgnoreCaseOrderByCreatedAtDesc(
                            filter.getStatus(),
                            filter.getArtisanName(),
                            pageable
                    );
        } else if (hasStatus) {
            // Filter by status only
            withdrawals = withdrawalRequestRepository.findByStatusOrderByCreatedAtDesc(
                    filter.getStatus(),
                    pageable
            );
        } else if (hasArtisanName) {
            // Filter by artisan name only
            withdrawals = withdrawalRequestRepository
                    .findByArtisan_Account_FullNameContainingIgnoreCaseOrderByCreatedAtDesc(
                            filter.getArtisanName(),
                            pageable
                    );
        } else if (hasDateRange) {
            // Filter by date range
            LocalDateTime fromDate = filter.getFromDate().atStartOfDay();
            LocalDateTime toDate = filter.getToDate().atTime(23, 59, 59);
            withdrawals = withdrawalRequestRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    fromDate,
                    toDate,
                    pageable
            );
        } else {
            // No filter - get all with PENDING first
            withdrawals = withdrawalRequestRepository.findAllOrderByStatusAndCreatedAt(pageable);
        }

        return withdrawals.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public WithdrawalResponse approveWithdrawal(UUID adminId, UUID withdrawalId, ApproveWithdrawalRequest request) {
        log.info("Approving withdrawal: {} by admin: {}", withdrawalId, adminId);

        // 1. Validate admin exists
        Account admin = accountRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        // 2. Lock withdrawal record (pessimistic lock)
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findByIdWithLock(withdrawalId)
                .orElseThrow(() -> new NotFoundException("Withdrawal request not found"));

        // 3. Validate status is PENDING
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new InvalidStatusException(
                    "Chỉ có thể phê duyệt yêu cầu rút tiền đang chờ xử lý. Trạng thái hiện tại: " + withdrawal.getStatus()
            );
        }

        // 4. Validate wallet balance is still sufficient
        Wallet wallet = walletRepository.findByAccount(withdrawal.getArtisan().getAccount())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        // Check available balance (total - locked)
        BigDecimal availableBalance = wallet.getAvailableBalance();

        if (availableBalance.compareTo(withdrawal.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Số dư khả dụng không đủ. Số dư khả dụng: %s VND, Số dư bị khóa: %s VND, Số tiền yêu cầu: %s VND",
                            availableBalance, wallet.getLockedBalance(), withdrawal.getAmount())
            );
        }

        // 5. Deduct money from wallet
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(withdrawal.getAmount());
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        // 6. Create WalletTransaction with type WITHDRAW
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(WalletTransactionType.WITHDRAW);
        transaction.setAmount(withdrawal.getAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(String.format("Rút tiền về tài khoản %s - %s",
                bankAccountUtil.mask(withdrawal.getBankAccountNumber()), withdrawal.getBankName()));
        transaction.setCreatedAt(LocalDateTime.now());

        transaction = walletTransactionRepository.save(transaction);

        // 7. Update withdrawal status to APPROVED
        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        withdrawal.setProcessedBy(admin);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setWalletTransaction(transaction);

        withdrawal = withdrawalRequestRepository.save(withdrawal);

        // 8. Log audit
        auditLogger.info("WITHDRAWAL_APPROVED: withdrawalId={}, adminId={}, amount={}",
                withdrawal.getWithdrawalId(), adminId, withdrawal.getAmount());

        log.info("Withdrawal approved: {}", withdrawal.getWithdrawalId());

        // 9. Send notification to Artisan
        try {
            notificationService.notifyArtisanOfWithdrawalApproval(
                    withdrawal.getArtisan().getAccount().getAccountId(),
                    withdrawal.getWithdrawalId(),
                    withdrawal.getAmount().longValue()
            );
        } catch (Exception e) {
            log.error("Failed to send notification to artisan", e);
        }

        return mapToResponse(withdrawal);
    }

    @Override
    @Transactional
    public WithdrawalResponse rejectWithdrawal(UUID adminId, UUID withdrawalId, RejectWithdrawalRequest request) {
        log.info("Rejecting withdrawal: {} by admin: {}", withdrawalId, adminId);

        // 1. Validate admin exists
        Account admin = accountRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        // 2. Find withdrawal
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new NotFoundException("Withdrawal request not found"));

        // 3. Validate status is PENDING
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new InvalidStatusException(
                    "Chỉ có thể từ chối yêu cầu rút tiền đang chờ xử lý. Trạng thái hiện tại: " + withdrawal.getStatus()
            );
        }

        // 4. Validate rejection reason is not empty (already validated by @Valid)

        // 5. Update status to REJECTED
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setRejectionReason(request.getRejectionReason());
        withdrawal.setProcessedBy(admin);
        withdrawal.setProcessedAt(LocalDateTime.now());

        withdrawal = withdrawalRequestRepository.save(withdrawal);

        // 6. Log audit
        auditLogger.info("WITHDRAWAL_REJECTED: withdrawalId={}, adminId={}, reason={}",
                withdrawal.getWithdrawalId(), adminId, request.getRejectionReason());

        log.info("Withdrawal rejected: {}", withdrawal.getWithdrawalId());

        // 7. Send notification to Artisan with reason
        try {
            notificationService.notifyArtisanOfWithdrawalRejection(
                    withdrawal.getArtisan().getAccount().getAccountId(),
                    withdrawal.getWithdrawalId(),
                    withdrawal.getAmount().longValue(),
                    request.getRejectionReason()
            );
        } catch (Exception e) {
            log.error("Failed to send notification to artisan", e);
        }

        return mapToResponse(withdrawal);
    }

    @Override
    public WithdrawalDetailResponse getWithdrawalDetail(UUID userId, UUID withdrawalId, boolean isAdmin) {
        log.info("Getting withdrawal detail: {} for user: {}", withdrawalId, userId);

        // 1. Find withdrawal
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new NotFoundException("Withdrawal request not found"));

        // 2. Validate access rights
        boolean isOwner = withdrawal.getArtisan().getAccount().getAccountId().equals(userId);

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to view this withdrawal");
        }

        // 3. Build detail response
        WithdrawalDetailResponse response = WithdrawalDetailResponse.builder()
                .withdrawalId(withdrawal.getWithdrawalId())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus())
                .bankName(withdrawal.getBankName())
                .bankAccountName(withdrawal.getBankAccountName())
                .createdAt(withdrawal.getCreatedAt())
                .processedAt(withdrawal.getProcessedAt())
                .rejectionReason(withdrawal.getRejectionReason())
                .reason(withdrawal.getReason())
                .cancelledAt(withdrawal.getCancelledAt())
                .artisanId(withdrawal.getArtisan().getArtisanUuid())
                .artisanName(withdrawal.getArtisan().getAccount().getFullName())
                .artisanEmail(withdrawal.getArtisan().getAccount().getEmail())
                .build();

        // 4. Mask or show full bank account number
        if (isOwner || isAdmin) {
            response.setFullBankAccountNumber(withdrawal.getBankAccountNumber());
            response.setBankAccountNumber(bankAccountUtil.mask(withdrawal.getBankAccountNumber()));
        } else {
            response.setBankAccountNumber(bankAccountUtil.mask(withdrawal.getBankAccountNumber()));
        }

        // 5. Include processedBy info if processed
        if (withdrawal.getProcessedBy() != null) {
            response.setProcessedByName(withdrawal.getProcessedBy().getFullName());
            response.setProcessedById(withdrawal.getProcessedBy().getAccountId());
        }

        // 6. Include walletTransaction info if approved
        if (withdrawal.getWalletTransaction() != null) {
            response.setWalletTransactionId(withdrawal.getWalletTransaction().getTransactionId());
        }

        return response;
    }

    /**
     * Helper method to map WithdrawalRequest to WithdrawalResponse
     */
    private WithdrawalResponse mapToResponse(WithdrawalRequest withdrawal) {
        return WithdrawalResponse.builder()
                .withdrawalId(withdrawal.getWithdrawalId())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus())
                .bankName(withdrawal.getBankName())
                .bankAccountNumber(bankAccountUtil.mask(withdrawal.getBankAccountNumber()))
                .bankAccountName(withdrawal.getBankAccountName())
                .createdAt(withdrawal.getCreatedAt())
                .processedAt(withdrawal.getProcessedAt())
                .processedByName(withdrawal.getProcessedBy() != null
                        ? withdrawal.getProcessedBy().getFullName()
                        : null)
                .rejectionReason(withdrawal.getRejectionReason())
                .reason(withdrawal.getReason())
                .artisanId(withdrawal.getArtisan().getArtisanUuid())
                .artisanName(withdrawal.getArtisan().getAccount().getFullName())
                .artisanEmail(withdrawal.getArtisan().getAccount().getEmail())
                .build();
    }
    
    /**
     * Calculate average days until locked balance is unlocked
     * Used for better UX message in withdrawal error
     */
    private int calculateDaysUntilUnlock(UUID artisanId) {
        try {
            // Find orders with unlock date in the future
            List<Order> ordersWithLock = orderRepository.findByArtisanIdAndUnlockDateAfter(
                artisanId, LocalDateTime.now()
            );
            
            if (ordersWithLock.isEmpty()) {
                return 0;
            }
            
            // Calculate average days until unlock
            long totalDays = ordersWithLock.stream()
                .mapToLong(order -> {
                    long days = java.time.Duration.between(
                        LocalDateTime.now(), 
                        order.getUnlockDate()
                    ).toDays();
                    return Math.max(0, days);
                })
                .sum();
            
            return (int) Math.ceil((double) totalDays / ordersWithLock.size());
        } catch (Exception e) {
            log.warn("Could not calculate days until unlock: {}", e.getMessage());
            return 7; // Default to 7 days if calculation fails
        }
    }
}
