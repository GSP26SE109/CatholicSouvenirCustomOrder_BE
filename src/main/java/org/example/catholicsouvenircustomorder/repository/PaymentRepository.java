package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    // Find by entity relationships
    List<Payment> findByOrder(Order order);
    
    List<Payment> findByCustomOrder(CustomOrder customOrder);
    
    List<Payment> findByStage(CustomOrderStage stage);
    
    // Find by IDs (for unified queries)
    List<Payment> findByOrder_OrderId(UUID orderId);
    
    List<Payment> findByCustomOrder_CustomOrderId(UUID customOrderId);
    
    List<Payment> findByStage_StageId(UUID stageId);
    
    // Find by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);
    
    // Find by status
    List<Payment> findByStatus(PaymentStatus status);
    
    // Find specific payment by all three IDs (for checking specific payment)
    @Query("SELECT p FROM Payment p WHERE " +
           "(:orderId IS NULL OR p.order.orderId = :orderId) AND " +
           "(:customOrderId IS NULL OR p.customOrder.customOrderId = :customOrderId) AND " +
           "(:stageId IS NULL OR p.stage.stageId = :stageId)")
    Optional<Payment> findByOrderIdAndCustomOrderIdAndStageId(
            @Param("orderId") UUID orderId,
            @Param("customOrderId") UUID customOrderId,
            @Param("stageId") UUID stageId);
    
    // Check if fully paid
    @Query("SELECT CASE WHEN COUNT(p) > 0 AND SUM(CASE WHEN p.status = 'SUCCESS' THEN p.amount ELSE 0 END) >= :totalAmount THEN true ELSE false END " +
           "FROM Payment p WHERE p.order = :order")
    boolean isOrderFullyPaid(@Param("order") Order order, @Param("totalAmount") java.math.BigDecimal totalAmount);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 AND SUM(CASE WHEN p.status = 'SUCCESS' THEN p.amount ELSE 0 END) >= :totalAmount THEN true ELSE false END " +
           "FROM Payment p WHERE p.customOrder = :customOrder")
    boolean isCustomOrderFullyPaid(@Param("customOrder") CustomOrder customOrder, @Param("totalAmount") java.math.BigDecimal totalAmount);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 AND SUM(CASE WHEN p.status = 'SUCCESS' THEN p.amount ELSE 0 END) >= :amount THEN true ELSE false END " +
           "FROM Payment p WHERE p.stage = :stage")
    boolean isStageFullyPaid(@Param("stage") CustomOrderStage stage, @Param("amount") java.math.BigDecimal amount);
}

