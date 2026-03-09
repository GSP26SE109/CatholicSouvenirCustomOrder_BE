package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Quotation;
import org.example.catholicsouvenircustomorder.model.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, UUID> {
    List<Quotation> findByCustomRequest_RequestId(UUID requestId);
    List<Quotation> findByArtisan_ArtisanUuid(UUID artisanId);
    Optional<Quotation> findByCustomRequest_RequestIdAndArtisan_ArtisanUuidAndStatus(
        UUID requestId, UUID artisanId, QuotationStatus status);
}
