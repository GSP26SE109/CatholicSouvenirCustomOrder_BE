package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.OrderTemplateDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderTemplateDetailRepository extends JpaRepository<OrderTemplateDetail, UUID> {
    List<OrderTemplateDetail> findByOrderOrderId(UUID orderId);
    List<OrderTemplateDetail> findByTemplateTemplateId(UUID templateId);
}
