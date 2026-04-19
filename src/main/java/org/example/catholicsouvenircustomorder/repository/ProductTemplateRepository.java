package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.TopTemplateDTO;
import org.example.catholicsouvenircustomorder.model.ProductTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get total count of templates for an artisan
     * Requirements: 5.1, 7.7
     */
    @Query("SELECT COUNT(pt) " +
           "FROM ProductTemplate pt " +
           "WHERE pt.artisan.artisanUuid = :artisanId")
    Long getTotalTemplatesCount(@Param("artisanId") UUID artisanId);
    
    /**
     * Get top templates by order count and revenue
     * Requirements: 5.2, 5.3, 7.7
     */
    @Query("SELECT pt.templateId as templateId, " +
           "pt.name as templateName, " +
           "COUNT(otd) as totalOrders, " +
           "COALESCE(SUM(otd.unitPrice * otd.quantity), 0) as totalRevenue " +
           "FROM ProductTemplate pt " +
           "LEFT JOIN OrderTemplateDetail otd ON otd.template.templateId = pt.templateId " +
           "WHERE pt.artisan.artisanUuid = :artisanId " +
           "GROUP BY pt.templateId, pt.name " +
           "ORDER BY totalOrders DESC")
    List<TopTemplateDTO> getTopTemplates(@Param("artisanId") UUID artisanId, Pageable pageable);
    
    /**
     * Get template conversion rate (templates with orders / total templates)
     * Requirements: 5.4, 7.7
     */
    @Query("SELECT " +
           "(CAST(COUNT(DISTINCT CASE WHEN EXISTS " +
           "(SELECT 1 FROM OrderTemplateDetail otd WHERE otd.template.templateId = pt.templateId) " +
           "THEN pt.templateId END) AS DOUBLE) * 100.0 / NULLIF(COUNT(pt), 0)) as conversionRate " +
           "FROM ProductTemplate pt " +
           "WHERE pt.artisan.artisanUuid = :artisanId")
    Double getTemplateConversionRate(@Param("artisanId") UUID artisanId);
}
