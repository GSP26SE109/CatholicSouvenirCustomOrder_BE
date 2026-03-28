package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.ProductTemplate;
import org.example.catholicsouvenircustomorder.model.TemplateCustomZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateCustomZoneRepository extends JpaRepository<TemplateCustomZone, UUID> {
    
    // Find zones by template ordered by sortOrder
    List<TemplateCustomZone> findByTemplateOrderBySortOrder(ProductTemplate template);
    
    List<TemplateCustomZone> findByTemplate_TemplateIdOrderBySortOrder(UUID templateId);
    
    // Check if sortOrder exists for a template
    boolean existsByTemplateAndSortOrder(ProductTemplate template, Integer sortOrder);
    
    Optional<TemplateCustomZone> findByTemplateAndSortOrder(ProductTemplate template, Integer sortOrder);
}
