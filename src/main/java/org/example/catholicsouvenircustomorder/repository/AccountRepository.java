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
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get top customers for a specific artisan by total spending
     * Requirements: 4.4, 4.5, 7.2
     */
    @Query("SELECT a.accountId as customerId, " +
           "a.fullName as customerName, " +
           "a.email as email, " +
           "COUNT(DISTINCT o.orderId) + COUNT(DISTINCT co.customOrderId) as totalOrders, " +
           "COALESCE(SUM(o.total), 0) + COALESCE(SUM(co.totalPrice), 0) as totalSpent " +
           "FROM Account a " +
           "LEFT JOIN Order o ON o.customer.accountId = a.accountId " +
           "LEFT JOIN o.orderDetails od ON od.order.orderId = o.orderId " +
           "LEFT JOIN CustomOrder co ON co.request.customer.accountId = a.accountId " +
           "WHERE (od.product.artisan.artisanUuid = :artisanId " +
           "OR co.artisan.artisanUuid = :artisanId) " +
           "AND (o.createAt >= :startDate OR co.createdAt >= :startDate) " +
           "GROUP BY a.accountId, a.fullName, a.email " +
           "ORDER BY totalSpent DESC")
    List<TopCustomerDTO> getArtisanTopCustomers(@Param("artisanId") UUID artisanId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 Pageable pageable);
    
    /**
     * Get customer statistics for a specific artisan
     * Requirements: 4.1, 4.2, 7.2
     */
    @Query("SELECT " +
           "COUNT(DISTINCT a.accountId) as totalCustomers, " +
           "(CAST(COUNT(DISTINCT CASE WHEN orderCount > 1 THEN a.accountId END) AS DOUBLE) * 100.0 / " +
           "NULLIF(COUNT(DISTINCT a.accountId), 0)) as repeatRate " +
           "FROM Account a " +
           "JOIN (SELECT o.customer.accountId as custId, COUNT(o) as orderCount " +
           "FROM Order o JOIN o.orderDetails od " +
           "WHERE od.product.artisan.artisanUuid = :artisanId " +
           "AND o.createAt >= :startDate " +
           "GROUP BY o.customer.accountId) subq " +
           "ON a.accountId = subq.custId")
    ArtisanCustomerStats getCustomerStats(@Param("artisanId") UUID artisanId,
                                          @Param("startDate") LocalDateTime startDate);
    
    /**
     * Interface projection for artisan customer statistics
     */
    interface ArtisanCustomerStats {
        Long getTotalCustomers();
        Double getRepeatRate();
    }
}
