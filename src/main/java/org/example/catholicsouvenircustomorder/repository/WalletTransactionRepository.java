package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.WalletBalanceTrend;
import org.example.catholicsouvenircustomorder.model.WalletTransaction;
import org.example.catholicsouvenircustomorder.model.WalletTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    
    List<WalletTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(UUID walletId);
    
    List<WalletTransaction> findByTypeOrderByCreatedAtDesc(WalletTransactionType type);
    
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM WalletTransaction wt " +
           "WHERE wt.wallet.walletId = :walletId AND wt.type = :type")
    BigDecimal getTotalAmountByWalletAndType(@Param("walletId") UUID walletId, 
                                              @Param("type") WalletTransactionType type);
    
    /**
     * Find all transactions with commission fee greater than 0 within a date range
     * Used for commission reporting
     */
    @Query("SELECT wt FROM WalletTransaction wt " +
           "WHERE wt.commissionFee > 0 " +
           "AND wt.createdAt >= :startDate " +
           "AND wt.createdAt <= :endDate " +
           "ORDER BY wt.createdAt ASC")
    List<WalletTransaction> findTransactionsWithCommissionInDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get financial summary for artisan including gross earnings and total commission
     * Requirements: 1.1, 1.2, 1.3, 7.1, 7.2, 7.3
     */
    @Query("SELECT " +
           "COALESCE(SUM(CASE WHEN wt.type = org.example.catholicsouvenircustomorder.model.WalletTransactionType.DEPOSIT " +
           "THEN wt.amount ELSE 0 END), 0) as grossEarnings, " +
           "COALESCE(SUM(wt.commissionFee), 0) as totalCommission " +
           "FROM WalletTransaction wt " +
           "WHERE wt.wallet.account.artisanProfile.artisanUuid = :artisanId " +
           "AND wt.createdAt >= :startDate")
    ArtisanFinancialSummary getFinancialSummary(@Param("artisanId") UUID artisanId, 
                                                 @Param("startDate") LocalDateTime startDate);
    
    /**
     * Get wallet balance trend aggregated by date
     * Requirements: 1.6, 7.1, 7.2, 7.3
     */
    @Query("SELECT CAST(wt.createdAt AS date) as date, " +
           "MAX(wt.balanceAfter) as balance " +
           "FROM WalletTransaction wt " +
           "WHERE wt.wallet.account.artisanProfile.artisanUuid = :artisanId " +
           "AND wt.createdAt >= :startDate " +
           "GROUP BY CAST(wt.createdAt AS date) " +
           "ORDER BY CAST(wt.createdAt AS date)")
    List<WalletBalanceTrend> getWalletBalanceTrend(@Param("artisanId") UUID artisanId,
                                                    @Param("startDate") LocalDateTime startDate);
    
    /**
     * Get current wallet balance from the most recent transaction
     * Requirements: 1.4, 7.1, 7.2
     */
    @Query("SELECT wt.balanceAfter " +
           "FROM WalletTransaction wt " +
           "WHERE wt.wallet.account.artisanProfile.artisanUuid = :artisanId " +
           "ORDER BY wt.createdAt DESC " +
           "LIMIT 1")
    BigDecimal getCurrentBalance(@Param("artisanId") UUID artisanId);
    
    /**
     * Interface projection for financial summary
     */
    interface ArtisanFinancialSummary {
        BigDecimal getGrossEarnings();
        BigDecimal getTotalCommission();
    }
    
    // ==================== Dashboard Statistics Methods ====================
    
    /**
     * Sum total platform commission earned after a specific date
     */
    @Query("SELECT COALESCE(SUM(wt.commissionFee), 0) FROM WalletTransaction wt " +
           "WHERE wt.type = 'PLATFORM_FEE' AND wt.createdAt >= :startDate")
    BigDecimal sumPlatformCommission(@Param("startDate") LocalDateTime startDate);
}
