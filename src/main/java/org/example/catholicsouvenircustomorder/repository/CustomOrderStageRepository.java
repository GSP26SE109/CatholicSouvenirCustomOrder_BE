package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.CustomOrderStage;
import org.example.catholicsouvenircustomorder.model.StageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CustomOrderStageRepository extends JpaRepository<CustomOrderStage, UUID> {
    List<CustomOrderStage> findByCustomOrder_CustomOrderIdOrderByStageOrderAsc(UUID customOrderId);
    
    /**
     * Find completed stages that are older than the specified date and have not released balance yet
     */
    @Query("SELECT s FROM CustomOrderStage s WHERE s.status = :status " +
           "AND s.completedAt < :completedBefore " +
           "AND s.balanceReleased = false " +
           "AND s.isPaid = true")
    List<CustomOrderStage> findCompletedStagesForBalanceRelease(
        @Param("status") StageStatus status,
        @Param("completedBefore") LocalDateTime completedBefore
    );
}
