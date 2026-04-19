package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.ComplaintStatistics;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.model.Complaint;
import org.example.catholicsouvenircustomorder.model.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    
    /**
     * Check if complaint already exists for an order or custom order
     * Requirements: 11.2
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Complaint c " +
           "WHERE (c.order.orderId = :orderId OR c.customOrder.customOrderId = :customOrderId)")
    boolean existsByOrderOrCustomOrder(@Param("orderId") UUID orderId, 
                                       @Param("customOrderId") UUID customOrderId);
    
    /**
     * Find all complaints by customer with pagination
     * Requirements: 7.1
     */
    Page<Complaint> findByCustomer(Account customer, Pageable pageable);
    
    /**
     * Find all complaints by artisan with pagination
     * Requirements: 8.1
     */
    Page<Complaint> findByArtisan(Artisan artisan, Pageable pageable);
    
    /**
     * Find all complaints by status with pagination
     * Requirements: 3.1
     */
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    
    // Dashboard statistics methods
    
    /**
     * Get complaint statistics including counts by status and total refund amount
     * Requirements: 6.1, 6.2, 6.3, 6.4
     */
    @Query("""
        SELECT COUNT(c) as totalComplaints,
               SUM(CASE WHEN c.status IN ('PENDING', 'WAITING_RETURN', 'PROCESSING_REFUND') 
                        THEN 1 ELSE 0 END) as pendingComplaints,
               SUM(CASE WHEN c.status = 'APPROVED' 
                        THEN 1 ELSE 0 END) as approvedComplaints,
               SUM(CASE WHEN c.status = 'REJECTED' 
                        THEN 1 ELSE 0 END) as rejectedComplaints,
               COALESCE(SUM(c.refundAmount), 0) as totalRefundAmount
        FROM Complaint c
        WHERE c.createdAt >= :startDate
        """)
    ComplaintStatistics getComplaintStatistics(@Param("startDate") LocalDateTime startDate);
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get complaint rate for an artisan
     * Calculates percentage of complaints vs total orders
     * Requirements: 3.5, 7.7
     */
    @Query("SELECT " +
           "(CAST(COUNT(c) AS DOUBLE) * 100.0 / NULLIF(:totalOrders, 0)) as complaintRate " +
           "FROM Complaint c " +
           "WHERE (c.order.orderId IN " +
           "(SELECT o.orderId FROM Order o JOIN o.orderDetails od " +
           "WHERE od.product.artisan.artisanUuid = :artisanId) " +
           "OR c.customOrder.artisan.artisanUuid = :artisanId)")
    Double getComplaintRate(@Param("artisanId") UUID artisanId, 
                           @Param("totalOrders") Long totalOrders);
}
