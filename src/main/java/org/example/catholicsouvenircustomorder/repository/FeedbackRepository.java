package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    
    /**
     * Check if feedback already exists for an order
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Feedback f " +
           "WHERE f.order.orderId = :orderId AND f.customer.accountId = :customerId")
    boolean existsByOrderAndCustomer(@Param("orderId") UUID orderId, @Param("customerId") UUID customerId);
    
    /**
     * Check if feedback already exists for a custom order
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Feedback f " +
           "WHERE f.customOrder.customOrderId = :customOrderId AND f.customer.accountId = :customerId")
    boolean existsByCustomOrderAndCustomer(@Param("customOrderId") UUID customOrderId, @Param("customerId") UUID customerId);
    
    /**
     * Find feedback by order and customer
     */
    @Query("SELECT f FROM Feedback f WHERE f.order.orderId = :orderId AND f.customer.accountId = :customerId")
    Optional<Feedback> findByOrderAndCustomer(@Param("orderId") UUID orderId, @Param("customerId") UUID customerId);
    
    /**
     * Find feedback by custom order and customer
     */
    @Query("SELECT f FROM Feedback f WHERE f.customOrder.customOrderId = :customOrderId AND f.customer.accountId = :customerId")
    Optional<Feedback> findByCustomOrderAndCustomer(@Param("customOrderId") UUID customOrderId, @Param("customerId") UUID customerId);
    
    /**
     * Find feedback by order detail
     */
    @Query("SELECT f FROM Feedback f WHERE f.orderDetail.id = :orderDetailId")
    Optional<Feedback> findByOrderDetail(@Param("orderDetailId") UUID orderDetailId);
    
    /**
     * Find all feedbacks by customer with pagination
     */
    Page<Feedback> findByCustomer(Account customer, Pageable pageable);
    
    /**
     * Find all feedbacks by artisan with pagination
     */
    Page<Feedback> findByArtisan(Artisan artisan, Pageable pageable);
    
    /**
     * Calculate average rating for an artisan
     */
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.artisan.artisanUuid = :artisanId")
    Double calculateAverageRatingByArtisan(@Param("artisanId") UUID artisanId);
    
    /**
     * Count total feedbacks for an artisan
     */
    long countByArtisan(Artisan artisan);
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get rating statistics for an artisan including average rating and total reviews
     * Requirements: 3.1, 3.2, 7.6
     */
    @Query("SELECT " +
           "AVG(f.rating) as avgRating, " +
           "COUNT(f) as totalReviews " +
           "FROM Feedback f " +
           "WHERE f.artisan.artisanUuid = :artisanId")
    ArtisanRatingStats getRatingStats(@Param("artisanId") UUID artisanId);
    
    /**
     * Interface projection for rating statistics
     */
    interface ArtisanRatingStats {
        Double getAvgRating();
        Long getTotalReviews();
    }
}
