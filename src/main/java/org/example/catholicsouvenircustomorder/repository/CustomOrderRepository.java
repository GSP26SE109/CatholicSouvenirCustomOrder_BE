package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.CustomOrder;
import org.example.catholicsouvenircustomorder.model.CustomOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomOrderRepository extends JpaRepository<CustomOrder, UUID> {
    List<CustomOrder> findByCustomer_AccountId(UUID customerId);
    List<CustomOrder> findByArtisan_ArtisanUuid(UUID artisanId);
    List<CustomOrder> findByStatus(CustomOrderStatus status);
    Optional<CustomOrder> findByCustomRequest_RequestId(UUID requestId);
}
