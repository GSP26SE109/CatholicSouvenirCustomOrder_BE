package org.example.catholicsouvenircustomorder.repository;

import jakarta.persistence.LockModeType;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.CustomRequest;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;
import org.example.catholicsouvenircustomorder.model.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
    
    /**
     * Find all requests by customer (no pagination)
     */
    List<CustomRequest> findByCustomer_AccountId(UUID customerId);
    
    /**
     * Find requests by status (no pagination)
     */
    List<CustomRequest> findByStatus(CustomRequestStatus status);
    
    /**
     * Find requests by selected artisan (no pagination)
     */
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
     * DEPRECATED: Template field removed from CustomRequest
     * Use findBySelectedArtisan_ArtisanUuidOrderByCreatedAtDesc instead
     */
    @Deprecated
    default Page<CustomRequest> findByTemplate_Artisan_ArtisanUuid(UUID artisanId, Pageable pageable) {
        return findBySelectedArtisan_ArtisanUuidOrderByCreatedAtDesc(artisanId, pageable);
    }
    
    /**
     * DEPRECATED: Template field removed from CustomRequest
     * Use findBySelectedArtisan_ArtisanUuidAndStatus instead
     */
    @Deprecated
    default Page<CustomRequest> findByTemplate_Artisan_ArtisanUuidAndStatus(
        UUID artisanId, 
        CustomRequestStatus status, 
        Pageable pageable
    ) {
        return findBySelectedArtisan_ArtisanUuidAndStatus(artisanId, status, pageable);
    }
    
    /**
     * DEPRECATED: Template field removed from CustomRequest
     * Use findBySelectedArtisan_ArtisanUuidAndStatus with PENDING status instead
     */
    @Deprecated
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.selectedArtisan.artisanUuid = :artisanId " +
           "AND cr.status = 'PENDING' ORDER BY cr.createdAt DESC")
    List<CustomRequest> findPendingRequestsByArtisan(@Param("artisanId") UUID artisanId);
    
    // ==================== Status Query Methods ====================
    
    /**
     * Find requests by status with pagination
     */
    Page<CustomRequest> findByStatusOrderByCreatedAtDesc(CustomRequestStatus status, Pageable pageable);
    
    /**
     * Count requests by status
     */
    long countByStatus(CustomRequestStatus status);
    
    /**
     * DEPRECATED: Template field removed from CustomRequest
     * Use countBySelectedArtisanAndStatus instead
     */
    @Deprecated
    @Query("SELECT COUNT(cr) FROM CustomRequest cr WHERE cr.selectedArtisan.artisanUuid = :artisanId " +
           "AND cr.status = 'PENDING'")
    long countPendingRequestsByArtisan(@Param("artisanId") UUID artisanId);
    
    /**
     * Count requests by selected artisan and status
     */
    @Query("SELECT COUNT(cr) FROM CustomRequest cr WHERE cr.selectedArtisan.artisanUuid = :artisanId " +
           "AND cr.status = :status")
    long countBySelectedArtisanAndStatus(@Param("artisanId") UUID artisanId, @Param("status") CustomRequestStatus status);
    
    // ==================== Template Query Methods ====================
    
    /**
     * DEPRECATED: Template field removed from CustomRequest
     * This method is no longer applicable for request-based flow
     */
    @Deprecated
    default Page<CustomRequest> findByTemplateId(UUID templateId, Pageable pageable) {
        return Page.empty();
    }
    
    /**
     * DEPRECATED: Template field removed from CustomRequest
     * This method is no longer applicable for request-based flow
     */
    @Deprecated
    default Optional<CustomRequest> findActiveRequestByCustomerAndTemplate(
        UUID customerId,
        UUID templateId
    ) {
        return Optional.empty();
    }
    
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
     * DEPRECATED: Template field removed from CustomRequest
     * Use countBySelectedArtisan instead
     */
    @Deprecated
    @Query("SELECT COUNT(cr) FROM CustomRequest cr WHERE cr.selectedArtisan.artisanUuid = :artisanId")
    long countByArtisan(@Param("artisanId") UUID artisanId);
    
    /**
     * Count requests where artisan is selected
     */
    @Query("SELECT COUNT(cr) FROM CustomRequest cr WHERE cr.selectedArtisan.artisanUuid = :artisanId")
    long countBySelectedArtisan(@Param("artisanId") UUID artisanId);
    
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
     * Find requests where artisan is selected (Request-Based flow) with pagination
     */
    Page<CustomRequest> findBySelectedArtisan_ArtisanUuidOrderByCreatedAtDesc(UUID artisanId, Pageable pageable);
    
    /**
     * Find requests where artisan is selected with status filter
     */
    Page<CustomRequest> findBySelectedArtisan_ArtisanUuidAndStatus(
        UUID artisanId, 
        CustomRequestStatus status, 
        Pageable pageable
    );
    
    /**
     * Find all requests for an artisan (Request-Based flow only)
     * Returns requests where artisan is selected
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE " +
           "cr.selectedArtisan.artisanUuid = :artisanId " +
           "ORDER BY cr.createdAt DESC")
    Page<CustomRequest> findAllByArtisan(@Param("artisanId") UUID artisanId, Pageable pageable);
    
    /**
     * Find all requests for an artisan with status filter (Request-Based flow only)
     * Returns requests where artisan is selected with specific status
     */
    @Query("SELECT cr FROM CustomRequest cr WHERE " +
           "cr.selectedArtisan.artisanUuid = :artisanId AND " +
           "cr.status = :status " +
           "ORDER BY cr.createdAt DESC")
    Page<CustomRequest> findAllByArtisanAndStatus(
        @Param("artisanId") UUID artisanId,
        @Param("status") CustomRequestStatus status,
        Pageable pageable
    );
    
    // ==================== Pessimistic Locking for Race Condition Prevention ====================
    
    /**
     * Find request by ID with pessimistic write lock
     * Used when creating CustomOrder to prevent race conditions
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cr FROM CustomRequest cr WHERE cr.requestId = :id")
    Optional<CustomRequest> findByIdWithLock(@Param("id") UUID id);
    
    /**
     * Check if customer has active request (DRAFT, OPEN, ARTISAN_SELECTED, IN_PROGRESS)
     */
    @Query("SELECT COUNT(cr) > 0 FROM CustomRequest cr WHERE cr.customer.accountId = :customerId " +
           "AND cr.status IN ('DRAFT', 'OPEN', 'ARTISAN_SELECTED', 'IN_PROGRESS')")
    boolean hasActiveRequest(@Param("customerId") UUID customerId);
}
