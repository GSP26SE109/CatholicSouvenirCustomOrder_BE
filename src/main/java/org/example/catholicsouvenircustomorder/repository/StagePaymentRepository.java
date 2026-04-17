package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.PaymentStatus;
import org.example.catholicsouvenircustomorder.model.StagePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StagePaymentRepository extends JpaRepository<StagePayment, UUID> {
    
    // Find by reference ID (internal tracking)
    Optional<StagePayment> findByReferenceId(String referenceId);
    
    // Find by transaction ID (from gateway)
    Optional<StagePayment> findByTransactionId(String transactionId);
    
    List<StagePayment> findByStage_StageId(UUID stageId);
    
    Optional<StagePayment> findByStage_StageIdAndStatus(UUID stageId, PaymentStatus status);
    
    List<StagePayment> findByStage_StageIdOrderByCreatedAtDesc(UUID stageId);
    
    // Find successful payment for a stage
    Optional<StagePayment> findFirstByStage_StageIdAndStatusOrderByPaidAtDesc(UUID stageId, PaymentStatus status);
    
    // Fetch stage payment with all necessary data for distribution (avoid lazy loading)
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT sp FROM StagePayment sp " +
           "LEFT JOIN FETCH sp.stage s " +
           "LEFT JOIN FETCH s.customOrder co " +
           "LEFT JOIN FETCH co.request cr " +
           "LEFT JOIN FETCH cr.selectedArtisan a " +
           "WHERE sp.paymentId = :paymentId")
    Optional<StagePayment> findByIdWithDetailsForDistribution(@org.springframework.data.repository.query.Param("paymentId") UUID paymentId);
}
