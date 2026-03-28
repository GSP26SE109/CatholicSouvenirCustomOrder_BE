package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.CreateCategoryRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateCategoryRequest;
import org.example.catholicsouvenircustomorder.dto.response.CategoryResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Category;
import org.example.catholicsouvenircustomorder.repository.CategoryRepository;
import org.example.catholicsouvenircustomorder.repository.ProductTemplateRepository;
import org.example.catholicsouvenircustomorder.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImp implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final ProductTemplateRepository productTemplateRepository;
    
    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating category: {}", request.getCategoryName());
        
        // Check if category name already exists
        if (categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new BadRequestException("Danh mục '" + request.getCategoryName() + "' đã tồn tại");
        }
        
        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive());
        category.setSortOrder(request.getSortOrder());
        category.setIconUrl(request.getIconUrl());
        
        category = categoryRepository.save(category);
        log.info("Created category with ID: {}", category.getCategoryId());
        
        return mapToResponse(category);
    }
    
    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        log.info("Updating category: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        
        // Update fields if provided
        if (request.getCategoryName() != null) {
            // Check if new name conflicts with existing category
            if (!category.getCategoryName().equals(request.getCategoryName()) &&
                categoryRepository.existsByCategoryName(request.getCategoryName())) {
                throw new BadRequestException("Tên danh mục '" + request.getCategoryName() + "' đã tồn tại");
            }
            category.setCategoryName(request.getCategoryName());
        }
        
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }
        
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        
        if (request.getIconUrl() != null) {
            category.setIconUrl(request.getIconUrl());
        }
        
        category = categoryRepository.save(category);
        log.info("Updated category: {}", categoryId);
        
        return mapToResponse(category);
    }
    
    @Override
    @Transactional
    public void deleteCategory(UUID categoryId) {
        log.info("Deleting category: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        
        // Soft delete by setting isActive = false
        category.setIsActive(false);
        categoryRepository.save(category);
        
        log.info("Deleted (soft) category: {}", categoryId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        log.debug("Fetching all active categories");
        
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        log.debug("Fetching all categories (no hierarchy)");
        
        // Since we removed hierarchy, just return all active categories
        return getAllActiveCategories();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubCategories(UUID parentCategoryId) {
        log.debug("SubCategories not supported - returning empty list");
        
        // Hierarchy removed, return empty list
        return List.of();
    }
    
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID categoryId) {
        log.debug("Fetching category by ID: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        
        return mapToResponse(category);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryByName(String categoryName) {
        log.debug("Fetching category by name: {}", categoryName);
        
        Category category = categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục: " + categoryName));
        
        return mapToResponse(category);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countTemplatesByCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        
        return productTemplateRepository.countByCategoryAndIsActiveTrue(category);
    }
    
    // ==================== Private Helper Methods ====================
    
    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .sortOrder(category.getSortOrder())
                .iconUrl(category.getIconUrl())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
        
        // Add template count
        response.setTemplateCount(productTemplateRepository.countByCategoryAndIsActiveTrue(category));
        
        return response;
    }
}
