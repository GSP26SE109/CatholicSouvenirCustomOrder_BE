package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.CustomOrderStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomOrderStageRepository extends JpaRepository<CustomOrderStage, UUID> {
    List<CustomOrderStage> findByCustomOrder_OrderIdOrderByStageOrderAsc(UUID orderId);
}
