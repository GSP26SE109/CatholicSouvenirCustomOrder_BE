package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.ComplaintEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintEvidenceRepository extends JpaRepository<ComplaintEvidence, UUID> {
}
