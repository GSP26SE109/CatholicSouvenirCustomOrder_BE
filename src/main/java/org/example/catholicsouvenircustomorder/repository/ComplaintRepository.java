package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
}
