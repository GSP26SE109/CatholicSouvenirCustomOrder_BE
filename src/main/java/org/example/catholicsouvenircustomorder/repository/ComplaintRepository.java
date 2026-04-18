package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.model.Complaint;
import org.example.catholicsouvenircustomorder.model.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
