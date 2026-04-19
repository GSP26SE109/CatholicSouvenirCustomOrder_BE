package org.example.catholicsouvenircustomorder.repository;

import jakarta.persistence.LockModeType;
import org.example.catholicsouvenircustomorder.model.WithdrawalRequest;
import org.example.catholicsouvenircustomorder.model.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {
    
    /**
     * Find all withdrawal requests by artisan ID, ordered by creation date descending
     * @param artisanUuid The artisan's UUID
     * @param pageable Pagination information
     * @return Page of withdrawal requests
     */
    Page<WithdrawalRequest> findByArtisan_ArtisanUuidOrderByCreatedAtDesc(UUID artisanUuid, Pageable pageable);
    
    /**
     * Find all withdrawal requests by status, ordered by creation date descending
     * @param status The withdrawal status
     * @param pageable Pagination information
     * @return Page of withdrawal requests
     */
    Page<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status, Pageable pageable);
    
    /**
     * Check if an artisan has a withdrawal request with a specific status
     * @param artisanUuid The artisan's UUID
     * @param status The withdrawal status to check
     * @return true if exists, false otherwise
     */
    boolean existsByArtisan_ArtisanUuidAndStatus(UUID artisanUuid, WithdrawalStatus status);
    
    /**
     * Find all withdrawal requests ordered by status (PENDING first) and creation date
     * @param pageable Pagination information
     * @return Page of withdrawal requests
     */
    @Query("SELECT wr FROM WithdrawalRequest wr " +
           "ORDER BY CASE WHEN wr.status = 'PENDING' THEN 0 ELSE 1 END, wr.createdAt DESC")
    Page<WithdrawalRequest> findAllOrderByStatusAndCreatedAt(Pageable pageable);
    
    /**
     * Find withdrawal requests by artisan name containing (case-insensitive)
     * @param fullName The artisan's full name to search
     * @param pageable Pagination information
     * @return Page of withdrawal requests
     */
    Page<WithdrawalRequest> findByArtisan_Account_FullNameContainingIgnoreCaseOrderByCreatedAtDesc(
        String fullName, Pageable pageable
    );
    
    /**
     * Find withdrawal requests by status and artisan name
     * @param status The withdrawal status
     * @param fullName The artisan's full name to search
     * @param pageable Pagination information
     * @return Page of withdrawal requests
     */
    Page<WithdrawalRequest> findByStatusAndArtisan_Account_FullNameContainingIgnoreCaseOrderByCreatedAtDesc(
        WithdrawalStatus status, String fullName, Pageable pageable
    );
    
    /**
     * Find withdrawal requests created between dates
     * @param fromDate Start date
     * @param toDate End date
     * @param pageable Pagination information
     * @return Page of withdrawal requests
     */
    Page<WithdrawalRequest> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable
    );
    
    /**
     * Find withdrawal request by ID with pessimistic write lock
     * This prevents concurrent modifications during approval/rejection
     * @param id The withdrawal request UUID
     * @return Optional containing the locked withdrawal request
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wr FROM WithdrawalRequest wr WHERE wr.withdrawalId = :id")
    Optional<WithdrawalRequest> findByIdWithLock(@Param("id") UUID id);
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get total pending withdrawal amount for an artisan
     * Requirements: 1.5
     */
    @Query("SELECT COALESCE(SUM(wr.amount), 0) " +
           "FROM WithdrawalRequest wr " +
           "WHERE wr.artisan.artisanUuid = :artisanId " +
           "AND wr.status = 'PENDING'")
    BigDecimal getPendingWithdrawalAmount(@Param("artisanId") UUID artisanId);
}
