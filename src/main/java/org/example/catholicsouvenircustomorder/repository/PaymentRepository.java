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
    
    // Find by order relationship
    List<Payment> findByOrder(Order order);
    
    List<Payment> findByOrderOrderId(UUID orderId);
    
    Optional<Payment> findByOrderOrderIdAndStatus(UUID orderId, PaymentStatus status);
    
    // Find by reference ID (internal tracking)
    Optional<Payment> findByReferenceId(String referenceId);
    
    // Find by transaction ID (from gateway)
    Optional<Payment> findByTransactionId(String transactionId);
    
    // Find by status
    List<Payment> findByStatus(PaymentStatus status);
    
    // Check if order is fully paid
    @Query("SELECT CASE WHEN COUNT(p) > 0 AND p.status = 'SUCCESS' THEN true ELSE false END " +
           "FROM Payment p WHERE p.order.orderId = :orderId")
    boolean isOrderFullyPaid(@Param("orderId") UUID orderId);
    
    // Get total paid amount for order
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.order.orderId = :orderId AND p.status = 'SUCCESS'")
    BigDecimal getTotalPaidAmount(@Param("orderId") UUID orderId);
}
