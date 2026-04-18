package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.Payment;
import org.example.catholicsouvenircustomorder.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    // Find by order group (NEW - primary method)
    List<Payment> findByOrderGroup_GroupId(UUID orderGroupId);
    
    Optional<Payment> findByOrderGroup_GroupIdAndStatus(UUID orderGroupId, PaymentStatus status);
    
    // Find by reference ID (internal tracking)
    Optional<Payment> findByReferenceId(String referenceId);
    
    // Find by transaction ID (from gateway)
    Optional<Payment> findByTransactionId(String transactionId);
    
    // Find by status
    List<Payment> findByStatus(PaymentStatus status);
    
    // Find by customer (through order group)
    @Query("SELECT p FROM Payment p WHERE p.orderGroup.customer.accountId = :customerId")
    List<Payment> findByCustomerId(@Param("customerId") UUID customerId);
    
    // Check if order group is fully paid
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Payment p WHERE p.orderGroup.groupId = :orderGroupId AND p.status = 'SUCCESS'")
    boolean isOrderGroupPaid(@Param("orderGroupId") UUID orderGroupId);
    
    // Get total paid amount for order group
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.orderGroup.groupId = :orderGroupId AND p.status = 'SUCCESS'")
    BigDecimal getTotalPaidAmount(@Param("orderGroupId") UUID orderGroupId);
    
    // Fetch payment with all necessary data for distribution (avoid lazy loading)
    @Query("SELECT DISTINCT p FROM Payment p " +
           "LEFT JOIN FETCH p.orderGroup og " +
           "LEFT JOIN FETCH og.orders o " +
           "WHERE p.paymentId = :paymentId")
    Optional<Payment> findByIdWithOrdersForDistribution(@Param("paymentId") UUID paymentId);
}
