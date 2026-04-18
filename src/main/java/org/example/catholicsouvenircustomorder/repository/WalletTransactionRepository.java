package org.example.catholicsouvenircustomorder.repository;

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
}
