package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.ProductTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductTemplateRepository extends JpaRepository<ProductTemplate, UUID> {
    // Page methods (with pagination) - ALL APIs use pagination
    Page<ProductTemplate> findByIsActiveTrue(Pageable pageable);
    Page<ProductTemplate> findByCategoryAndIsActiveTrue(org.example.catholicsouvenircustomorder.model.Category category, Pageable pageable);
    Page<ProductTemplate> findByCategory_CategoryIdAndIsActiveTrue(UUID categoryId, Pageable pageable);
    Page<ProductTemplate> findByArtisanAndIsActiveTrue(org.example.catholicsouvenircustomorder.model.Artisan artisan, Pageable pageable);
    Page<ProductTemplate> findByArtisan_ArtisanUuidAndIsActiveTrue(UUID artisanId, Pageable pageable);
    
    // Count methods
    long countByCategoryAndIsActiveTrue(org.example.catholicsouvenircustomorder.model.Category category);
    long countByCategory_CategoryIdAndIsActiveTrue(UUID categoryId);
}
