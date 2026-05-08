package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Complaint;
import org.example.catholicsouvenircustomorder.model.RefundStatus;
import org.example.catholicsouvenircustomorder.model.RefundTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, UUID> {
    
    /**
     * Find refund transaction by complaint
     * Requirements: 9.1
     */
    Optional<RefundTransaction> findByComplaint(Complaint complaint);
    
    /**
     * Find all refund transactions by status with pagination
     * Requirements: 9.1
     */
    Page<RefundTransaction> findByStatus(RefundStatus status, Pageable pageable);
    
    // ==================== Dashboard Statistics Methods ====================
    
    /**
     * Count total refunds after a specific date
     */
    @Query("SELECT COUNT(rt) FROM RefundTransaction rt WHERE rt.createdAt >= :startDate")
    Long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Sum total refund amount after a specific date
     */
    @Query("SELECT COALESCE(SUM(rt.amount), 0) FROM RefundTransaction rt WHERE rt.createdAt >= :startDate")
    BigDecimal sumAmountByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Count refunds by artisan after a specific date
     */
    @Query("SELECT COUNT(rt) FROM RefundTransaction rt " +
           "WHERE rt.fromWallet.account.accountId = :artisanId " +
           "AND rt.createdAt >= :startDate")
    Long countByArtisanAndCreatedAtAfter(@Param("artisanId") UUID artisanId, 
                                         @Param("startDate") LocalDateTime startDate);
    
    /**
     * Sum refund amount by artisan after a specific date
     */
    @Query("SELECT COALESCE(SUM(rt.amount), 0) FROM RefundTransaction rt " +
           "WHERE rt.fromWallet.account.accountId = :artisanId " +
           "AND rt.createdAt >= :startDate")
    BigDecimal sumAmountByArtisanAndCreatedAtAfter(@Param("artisanId") UUID artisanId, 
                                                    @Param("startDate") LocalDateTime startDate);
}
