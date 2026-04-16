package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.ApproveWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.RejectWithdrawalRequest;
import org.example.catholicsouvenircustomorder.dto.request.WithdrawalFilterRequest;
import org.example.catholicsouvenircustomorder.dto.response.WithdrawalDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.WithdrawalResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Service interface for managing withdrawal requests
 * Handles artisan withdrawal operations including creation, approval, rejection, and cancellation
 */
public interface WithdrawalService {
    
    /**
     * Create a new withdrawal request
     * @param artisanId The artisan's UUID
     * @param request The withdrawal request details
     * @return WithdrawalResponse containing the created withdrawal
     * @throws org.example.catholicsouvenircustomorder.exception.InsufficientBalanceException if wallet balance is not enough
     * @throws org.example.catholicsouvenircustomorder.exception.PendingWithdrawalExistsException if artisan has pending withdrawal
     */
    WithdrawalResponse createWithdrawalRequest(UUID artisanId, CreateWithdrawalRequest request);
    
    /**
     * Get all withdrawals for an artisan with filtering and pagination
     * @param artisanId The artisan's UUID
     * @param filter Filter criteria including status, date range, and pagination
     * @return Page of WithdrawalResponse
     */
    Page<WithdrawalResponse> getWithdrawalsByArtisan(UUID artisanId, WithdrawalFilterRequest filter);
    
    /**
     * Cancel a pending withdrawal request
     * @param artisanId The artisan's UUID
     * @param withdrawalId The withdrawal request UUID
     * @throws org.example.catholicsouvenircustomorder.exception.UnauthorizedException if not owner
     * @throws org.example.catholicsouvenircustomorder.exception.InvalidStatusException if status is not PENDING
     */
    void cancelWithdrawal(UUID artisanId, UUID withdrawalId);
    
    /**
     * Get all withdrawal requests (Admin only)
     * @param filter Filter criteria including status, artisan name, date range, and pagination
     * @return Page of WithdrawalResponse
     */
    Page<WithdrawalResponse> getAllWithdrawals(WithdrawalFilterRequest filter);
    
    /**
     * Approve a withdrawal request (Admin only)
     * @param adminId The admin's UUID
     * @param withdrawalId The withdrawal request UUID
     * @param request The approval request containing optional note
     * @return WithdrawalResponse containing the approved withdrawal
     * @throws org.example.catholicsouvenircustomorder.exception.InsufficientBalanceException if wallet balance is not enough
     * @throws org.example.catholicsouvenircustomorder.exception.InvalidStatusException if status is not PENDING
     */
    WithdrawalResponse approveWithdrawal(UUID adminId, UUID withdrawalId, ApproveWithdrawalRequest request);
    
    /**
     * Reject a withdrawal request (Admin only)
     * @param adminId The admin's UUID
     * @param withdrawalId The withdrawal request UUID
     * @param request The rejection request containing reason
     * @return WithdrawalResponse containing the rejected withdrawal
     * @throws org.example.catholicsouvenircustomorder.exception.InvalidStatusException if status is not PENDING
     */
    WithdrawalResponse rejectWithdrawal(UUID adminId, UUID withdrawalId, RejectWithdrawalRequest request);
    
    /**
     * Get withdrawal detail
     * @param userId The user's UUID (artisan or admin)
     * @param withdrawalId The withdrawal request UUID
     * @param isAdmin Whether the user is an admin
     * @return WithdrawalDetailResponse containing full withdrawal details
     * @throws org.example.catholicsouvenircustomorder.exception.UnauthorizedException if not owner or admin
     */
    WithdrawalDetailResponse getWithdrawalDetail(UUID userId, UUID withdrawalId, boolean isAdmin);
}
