package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.CustomerStatistics;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.TopCustomerDTO;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<Account> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    Page<Account> findByRole(Role role, Pageable pageable);
    List<Account> findByRole_Name(String roleName);
    Optional<Account> findByVerificationToken(String token);
    
    // Dashboard statistics methods
    
    /**
     * Get customer statistics including total, new, and active customers
     * Requirements: 1.1, 1.2, 1.3, 1.4
     */
    @Query("""
        SELECT COUNT(a) as totalCustomers,
               SUM(CASE WHEN a.createdDate >= :startDate THEN 1 ELSE 0 END) as newCustomers,
               SUM(CASE WHEN EXISTS (
                   SELECT 1 FROM Order o WHERE o.customer = a AND o.createAt >= :startDate
               ) THEN 1 ELSE 0 END) as activeCustomers
        FROM Account a
        WHERE a.role.name = 'CUSTOMER'
        """)
    CustomerStatistics getCustomerStatistics(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get top customers by total spending
     * Requirements: 2.1, 2.2, 2.3, 2.4
     */
    @Query("""
        SELECT CAST(a.accountId AS string) as customerId,
               a.fullName as customerName,
               a.email as email,
               COUNT(o) as totalOrders,
               COALESCE(SUM(o.total), 0) as totalSpent
        FROM Account a
        LEFT JOIN Order o ON o.customer = a
        WHERE a.role.name = 'CUSTOMER'
        GROUP BY a.accountId, a.fullName, a.email
        ORDER BY totalSpent DESC
        """)
    List<TopCustomerDTO> getTopCustomers(Pageable pageable);
}
