package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.StagePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StagePaymentRepository extends JpaRepository<StagePayment, UUID> {
    List<StagePayment> findByStage_StageId(UUID stageId);
    List<StagePayment> findByCustomer_AccountId(UUID customerId);
}
