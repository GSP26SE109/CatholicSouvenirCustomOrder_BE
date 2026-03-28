package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.CustomRequest;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;
import org.example.catholicsouvenircustomorder.model.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CustomRequest entity operations.
 * Provides query methods for template-based customization workflow.
 */
@Repository
public interface CustomRequestRepository extends JpaRepository<CustomRequest, UUID> {
    
    // ==================== Legacy Methods ====================
    
    List<CustomRequest> findByCustomer_AccountId(UUID customerId);
    
    List<CustomRequest> findByStatus(CustomRequestStatus status);
    
    // Fix: Changed from selectedArtisans to selectedArtisan (singular)
    List<CustomRequest> findBySelectedArtisan_ArtisanUuid(UUID artisanId);
    
    // ==================== Customer Query Methods ====================
    
    /**
     * Find all requests by customer with pagination
     */
    Page<CustomRequest> findByCustomer(Account customer, Pageable pageable);
    
    /**
     * Find requests by customer and status with pagination
     */
    Page<CustomRequest> findByCustomerAndStatus(Account customer, CustomRequestStatus status, Pageable pageable);
    
    /**
     * Find requests by customer ordered by creation date
     */
    Page<CustomRequest> findByCustomerOrderByCreatedAtDesc(Account customer, Pageable pageable);
    
    // ==================== Artisan Query Methods ====================
    
    /**
     * Find all requests for templates owned by artisan
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.template.artisan.artisanUuid = :artisanId")
    Page<CustomRequest> findByTemplate_Artisan_ArtisanUuid(@Param("artisanId") UUID artisanId, Pageable pageable);
    
    /**
     * Find requests for templates owned by artisan with status filter
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.template.artisan.artisanUuid = :artisanId AND cr.status = :status")
    Page<CustomRequest> findByTemplate_Artisan_ArtisanUuidAndStatus(
        @Param("artisanId") UUID artisanId, 
        @Param("status") CustomRequestStatus status, 
        Pageable pageable
    );
    
    /**
     * Find pending requests for artisan ordered by creation date
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.template.artisan.artisanUuid = :artisanId " +
           "AND cr.status = 'PENDING' ORDER BY cr.createdAt DESC")
    List<CustomRequest> findPendingRequestsByArtisan(@Param("artisanId") UUID artisanId);
    
    // ==================== Status Query Methods ====================
    
    /**
     * Find requests by status with pagination
     */
    Page<CustomRequest> findByStatus(CustomRequestStatus status, Pageable pageable);
    
    /**
     * Count requests by status
     */
    long countByStatus(CustomRequestStatus status);
    
    /**
     * Count pending requests for artisan
     */
    @Query("SELECT COUNT(cr) FROM CustomRequest cr WHERE cr.template.artisan.artisanUuid = :artisanId " +
           "AND cr.status = 'PENDING'")
    long countPendingRequestsByArtisan(@Param("artisanId") UUID artisanId);
    
    // ==================== Template Query Methods ====================
    
    /**
     * Find requests by template ID
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.template.templateId = :templateId")
    Page<CustomRequest> findByTemplateId(@Param("templateId") UUID templateId, Pageable pageable);
    
    /**
     * Check if customer has existing request for template
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.customer.accountId = :customerId " +
           "AND cr.template.templateId = :templateId AND cr.status IN ('PENDING', 'ACCEPTED')")
    Optional<CustomRequest> findActiveRequestByCustomerAndTemplate(
        @Param("customerId") UUID customerId,
        @Param("templateId") UUID templateId
    );
    
    // ==================== Analytics Query Methods ====================
    
    /**
     * Find requests created within date range
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.createdAt BETWEEN :startDate AND :endDate")
    List<CustomRequest> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Count requests by customer
     */
    long countByCustomer(Account customer);
    
    /**
     * Count requests for artisan's templates
     */
    @Query("SELECT COUNT(cr) FROM CustomRequest cr WHERE cr.template.artisan.artisanUuid = :artisanId")
    long countByArtisan(@Param("artisanId") UUID artisanId);
    
    // ==================== Request Type Query Methods ====================
    
    /**
     * Find requests by status and request type
     */
    Page<CustomRequest> findByStatusAndRequestType(
        CustomRequestStatus status, 
        RequestType requestType, 
        Pageable pageable
    );
    
    /**
     * Find requests by request type and status
     */
    Page<CustomRequest> findByRequestTypeAndStatus(
        RequestType requestType, 
        CustomRequestStatus status, 
        Pageable pageable
    );
    
    /**
     * Find open requests for bidding (Request-Based flow)
     * Returns all OPEN requests with REQUEST_BASED type
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.requestType = 'REQUEST_BASED' " +
           "AND cr.status = 'OPEN' ORDER BY cr.createdAt DESC")
    Page<CustomRequest> findOpenRequestsForBidding(Pageable pageable);
    
    /**
     * Find requests where artisan is selected (Request-Based flow)
     */
    Page<CustomRequest> findBySelectedArtisan_ArtisanUuid(UUID artisanId, Pageable pageable);
    
    Page<CustomRequest> findBySelectedArtisan_ArtisanUuidAndStatus(
        UUID artisanId, 
        CustomRequestStatus status, 
        Pageable pageable
    );
    
    /**
     * Find all requests for an artisan (both Template-Based and Request-Based)
     * Template-Based: where artisan owns the template
     * Request-Based: where artisan is selected
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE " +
           "(cr.template.artisan.artisanUuid = :artisanId) OR " +
           "(cr.selectedArtisan.artisanUuid = :artisanId) " +
           "ORDER BY cr.createdAt DESC")
    Page<CustomRequest> findAllByArtisan(@Param("artisanId") UUID artisanId, Pageable pageable);
    
    @Query("SELECT cr FROM CustomRequest cr WHERE " +
           "((cr.template.artisan.artisanUuid = :artisanId) OR " +
           "(cr.selectedArtisan.artisanUuid = :artisanId)) AND " +
           "cr.status = :status " +
           "ORDER BY cr.createdAt DESC")
    Page<CustomRequest> findAllByArtisanAndStatus(
        @Param("artisanId") UUID artisanId,
        @Param("status") CustomRequestStatus status,
        Pageable pageable
    );
    
    /**
     * Find requests by customer
     */
    List<CustomRequest> findByCustomer(Account customer);
}
