package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StageRepository extends JpaRepository<Stage, UUID> {
}
