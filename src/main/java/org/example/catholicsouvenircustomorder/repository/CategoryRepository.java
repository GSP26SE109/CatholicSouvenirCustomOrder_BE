package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entity operations.
 * Provides query methods for category management.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    /**
     * Find all active categories ordered by sort order
     */
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();
    
    /**
     * Find category by name
     */
    Optional<Category> findByCategoryName(String categoryName);
    
    /**
     * Check if category name exists
     */
    boolean existsByCategoryName(String categoryName);
    
    /**
     * Find all active categories (legacy method for backward compatibility)
     */
    List<Category> findByIsActiveTrue();
}
