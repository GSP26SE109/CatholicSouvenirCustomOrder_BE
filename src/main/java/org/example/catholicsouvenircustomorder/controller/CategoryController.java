package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateCategoryRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateCategoryRequest;
import org.example.catholicsouvenircustomorder.dto.response.CategoryResponse;
import org.example.catholicsouvenircustomorder.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for Category management.
 * Handles category CRUD operations and queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    // ==================== PUBLIC ENDPOINTS ====================
    
    /**
     * Get all active categories
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<BaseResponse> getAllCategories() {
        log.info("Fetching all active categories");
        
        List<CategoryResponse> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách danh mục thành công", categories));
    }
    
    /**
     * Get root categories with subcategories
     * GET /api/categories/root
     */
    @GetMapping("/root")
    public ResponseEntity<BaseResponse> getRootCategories() {
        log.info("Fetching root categories");
        
        List<CategoryResponse> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(BaseResponse.success("Lấy danh mục gốc thành công", categories));
    }
    
    /**
     * Get category by ID
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> getCategoryById(@PathVariable UUID id) {
        log.info("Fetching category by ID: {}", id);
        
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin danh mục thành công", category));
    }
    
    /**
     * Get subcategories of a parent category
     * GET /api/categories/{id}/subcategories
     */
    @GetMapping("/{id}/subcategories")
    public ResponseEntity<BaseResponse> getSubCategories(@PathVariable UUID id) {
        log.info("Fetching subcategories for parent: {}", id);
        
        List<CategoryResponse> subCategories = categoryService.getSubCategories(id);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh mục con thành công", subCategories));
    }
    
    /**
     * Get category by name
     * GET /api/categories/by-name/{name}
     */
    @GetMapping("/by-name/{name}")
    public ResponseEntity<BaseResponse> getCategoryByName(@PathVariable String name) {
        log.info("Fetching category by name: {}", name);
        
        CategoryResponse category = categoryService.getCategoryByName(name);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin danh mục thành công", category));
    }
    
    // ==================== ADMIN ENDPOINTS ====================
    
    /**
     * Create a new category (Admin only)
     * POST /api/categories
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ARTISAN')")
    public ResponseEntity<BaseResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        log.info("Admin creating category: {}", request.getCategoryName());
        
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.ok(BaseResponse.success("Tạo danh mục thành công", category));
    }
    
    /**
     * Update a category (Admin only)
     * PUT /api/categories/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        log.info("Admin updating category: {}", id);
        
        CategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật danh mục thành công", category));
    }
    
    /**
     * Delete a category (Admin only)
     * DELETE /api/categories/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse> deleteCategory(@PathVariable UUID id) {
        log.info("Admin deleting category: {}", id);
        
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(BaseResponse.success("Xóa danh mục thành công"));
    }
}
