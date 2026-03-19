package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.CustomRequest;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomRequestRepository extends JpaRepository<CustomRequest, UUID> {
    List<CustomRequest> findByCustomer_AccountId(UUID customerId);
    List<CustomRequest> findByStatus(CustomRequestStatus status);
    List<CustomRequest> findBySelectedArtisans_ArtisanUuid(UUID artisanId);
}
