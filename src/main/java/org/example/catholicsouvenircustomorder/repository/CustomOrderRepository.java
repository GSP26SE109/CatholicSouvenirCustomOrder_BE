package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.model.CustomOrder;
import org.example.catholicsouvenircustomorder.model.CustomOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CustomOrder entity operations.
 * Provides query methods for custom order management.
 */
@Repository
public interface CustomOrderRepository extends JpaRepository<CustomOrder, UUID> {
    
    /**
     * Find all orders by customer account ID (for payment queries)
     */
    @Query("SELECT co FROM CustomOrder co WHERE co.request.customer.accountId = :customerId")
    java.util.List<CustomOrder> findByRequest_Customer_AccountId(@Param("customerId") UUID customerId);
    
    /**
     * Find all orders by artisan UUID (for payment queries)
     */
    @Query("SELECT co FROM CustomOrder co WHERE co.artisan.artisanUuid = :artisanUuid")
    java.util.List<CustomOrder> findByArtisan_ArtisanUuid(@Param("artisanUuid") UUID artisanUuid);
    
    // ==================== Customer Query Methods ====================
    
    /**
     * Find all orders by customer with pagination
     */
    @Query("SELECT co FROM CustomOrder co WHERE co.request.customer = :customer")
    Page<CustomOrder> findByRequest_Customer(@Param("customer") Account customer, Pageable pageable);
    
    /**
     * Find orders by customer and status with pagination
     */
    @Query("SELECT co FROM CustomOrder co WHERE co.request.customer = :customer AND co.status = :status")
    Page<CustomOrder> findByRequest_CustomerAndStatus(
        @Param("customer") Account customer,
        @Param("status") CustomOrderStatus status,
        Pageable pageable
    );
    
    /**
     * Find orders by customer ordered by creation date
     */
    @Query("SELECT co FROM CustomOrder co WHERE co.request.customer = :customer ORDER BY co.createdAt DESC")
    Page<CustomOrder> findByRequest_CustomerOrderByCreatedAtDesc(@Param("customer") Account customer, Pageable pageable);
    
    // ==================== Artisan Query Methods ====================
    
    /**
     * Find all orders by artisan with pagination
     */
    Page<CustomOrder> findByArtisan(Artisan artisan, Pageable pageable);
    
    /**
     * Find orders by artisan and status with pagination
     */
    Page<CustomOrder> findByArtisanAndStatus(Artisan artisan, CustomOrderStatus status, Pageable pageable);
    
    /**
     * Find orders by artisan ordered by creation date
     */
    Page<CustomOrder> findByArtisanOrderByCreatedAtDesc(Artisan artisan, Pageable pageable);
    
    // ==================== Status Query Methods ====================
    
    /**
     * Find orders by status with pagination
     */
    Page<CustomOrder> findByStatus(CustomOrderStatus status, Pageable pageable);
    
    /**
     * Count orders by status
     */
    long countByStatus(CustomOrderStatus status);
    
    /**
     * Count orders by artisan
     */
    long countByArtisan(Artisan artisan);
    
    // ==================== Request Query Methods ====================
    
    /**
     * Find order by request ID
     */
    @Query("SELECT co FROM CustomOrder co WHERE co.request.requestId = :requestId")
    Optional<CustomOrder> findByRequestId(@Param("requestId") UUID requestId);
    
    /**
     * Check if order exists for request
     */
    @Query("SELECT CASE WHEN COUNT(co) > 0 THEN true ELSE false END FROM CustomOrder co WHERE co.request.requestId = :requestId")
    boolean existsByRequestId(@Param("requestId") UUID requestId);
    
    // Dashboard statistics methods
    
    /**
     * Get total custom order revenue within time range
     * Requirements: 7.1, 7.2, 7.3, 7.4
     */
    @Query("""
        SELECT COALESCE(SUM(co.totalPrice), 0)
        FROM CustomOrder co
        WHERE co.createdAt >= :startDate
        """)
    BigDecimal getCustomOrderRevenue(@Param("startDate") LocalDateTime startDate);
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get average completion time in days for completed custom orders
     * Uses updatedAt as completion timestamp when status is COMPLETED
     * Requirements: 2.4
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (co.updated_at - co.created_at)) / 86400) " +
           "FROM custom_orders co " +
           "JOIN artisans a ON co.artisan_id = a.artisan_id " +
           "WHERE a.artisan_uuid = :artisanId " +
           "AND co.status = 'COMPLETED' " +
           "AND co.created_at >= :startDate",
           nativeQuery = true)
    Double getAvgCompletionTimeDays(@Param("artisanId") UUID artisanId,
                                    @Param("startDate") LocalDateTime startDate);
}
