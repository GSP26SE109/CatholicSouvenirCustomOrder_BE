package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateCategoryRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateCategoryRequest;
import org.example.catholicsouvenircustomorder.dto.response.CategoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Category operations.
 * Handles category management for organizing templates and products.
 */
public interface CategoryService {
    
    /**
     * Create a new category
     */
    CategoryResponse createCategory(CreateCategoryRequest request);
    
    /**
     * Update an existing category
     */
    CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request);
    
    /**
     * Delete a category (soft delete by setting isActive = false)
     */
    void deleteCategory(UUID categoryId);
    
    /**
     * Get all active categories ordered by sort order
     */
    List<CategoryResponse> getAllActiveCategories();
    
    /**
     * Get root categories (no parent)
     */
    List<CategoryResponse> getRootCategories();
    
    /**
     * Get subcategories of a parent category
     */
    List<CategoryResponse> getSubCategories(UUID parentCategoryId);
    
    /**
     * Get category by ID
     */
    CategoryResponse getCategoryById(UUID categoryId);
    
    /**
     * Get category by name
     */
    CategoryResponse getCategoryByName(String categoryName);
    
    /**
     * Count templates in a category
     */
    long countTemplatesByCategory(UUID categoryId);
}
