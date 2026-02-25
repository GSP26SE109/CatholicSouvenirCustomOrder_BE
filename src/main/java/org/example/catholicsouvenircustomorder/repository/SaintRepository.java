package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Saint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SaintRepository extends JpaRepository<Saint, UUID> {
}
