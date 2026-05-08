package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    
    Optional<Wallet> findByAccount(Account account);
    
    Optional<Wallet> findByAccount_AccountId(UUID accountId);
    
    // Balance aggregation methods for dashboard
    @Query("SELECT COALESCE(SUM(w.lockedBalance), 0) FROM Wallet w")
    BigDecimal sumAllLockedBalances();
    
    @Query("SELECT COALESCE(SUM(w.balance - w.lockedBalance), 0) FROM Wallet w")
    BigDecimal sumAllAvailableBalances();
    
    @Query("SELECT COALESCE(w.lockedBalance, 0) FROM Wallet w WHERE w.account.accountId = :artisanId")
    BigDecimal getLockedBalanceByArtisanId(@Param("artisanId") UUID artisanId);
    
    @Query("SELECT COALESCE(w.balance - w.lockedBalance, 0) FROM Wallet w WHERE w.account.accountId = :artisanId")
    BigDecimal getAvailableBalanceByArtisanId(@Param("artisanId") UUID artisanId);
    
    // Get admin wallet balance (total balance including locked)
    @Query("SELECT COALESCE(w.balance, 0) FROM Wallet w WHERE w.account.role.name = 'ADMIN'")
    BigDecimal getAdminWalletBalance();
}
