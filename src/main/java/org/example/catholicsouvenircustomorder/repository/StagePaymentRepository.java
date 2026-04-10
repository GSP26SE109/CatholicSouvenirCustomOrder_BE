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
}
