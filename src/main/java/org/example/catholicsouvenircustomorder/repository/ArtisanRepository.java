package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.ArtisanStatistics;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.TopArtisanDTO;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtisanRepository extends JpaRepository<Artisan, UUID> {
    boolean existsByAccount(Account account);
    
    // Find non-blacklisted artisans
    Page<Artisan> findByIsBlacklistedFalse(Pageable pageable);
    
    @Query("SELECT a FROM Artisan a JOIN a.customOrders co WHERE co.customOrderId = :customOrderId")
    Optional<Artisan> findByCustomOrderId(@Param("customOrderId") UUID customOrderId);
    
    // Find artisan by product in order details
    @Query("SELECT DISTINCT p.artisan FROM Product p " +
           "JOIN OrderDetail od ON od.product.productId = p.productId " +
           "WHERE od.order.orderId = :orderId")
    Optional<Artisan> findByOrderIdFromProduct(@Param("orderId") UUID orderId);
    
    // Find artisan by template in order template details
    @Query("SELECT DISTINCT pt.artisan FROM ProductTemplate pt " +
           "JOIN OrderTemplateDetail otd ON otd.template.templateId = pt.templateId " +
           "WHERE otd.order.orderId = :orderId")
    Optional<Artisan> findByOrderIdFromTemplate(@Param("orderId") UUID orderId);
    
    // Dashboard statistics methods
    
    /**
     * Get artisan statistics including total, pending, and active artisans
     * Requirements: 3.1, 3.2, 3.3, 3.4
     */
    @Query("""
        SELECT COUNT(a) as totalArtisans,
               SUM(CASE WHEN aa.status = 'PENDING' THEN 1 ELSE 0 END) as pendingArtisans,
               SUM(CASE WHEN a.account.isVerified = true THEN 1 ELSE 0 END) as activeArtisans
        FROM Artisan a
        LEFT JOIN ArtisanApplication aa ON aa.account = a.account
        """)
    ArtisanStatistics getArtisanStatistics();
    
    /**
     * Get top artisans by total revenue
     * Requirements: 4.1, 4.2, 4.3, 4.4
     */
    @Query("""
        SELECT CAST(a.artisanUuid AS string) as artisanId,
               a.account.fullName as artisanName,
               COUNT(DISTINCT o) as totalOrders,
               COALESCE(SUM(o.total), 0) as totalRevenue,
               COALESCE(AVG(f.rating), 0.0) as averageRating
        FROM Artisan a
        LEFT JOIN Product p ON p.artisan = a
        LEFT JOIN OrderDetail od ON od.product = p
        LEFT JOIN Order o ON od.order = o
        LEFT JOIN Feedback f ON f.artisan = a
        GROUP BY a.artisanUuid, a.account.fullName
        ORDER BY totalRevenue DESC
        """)
    List<TopArtisanDTO> getTopArtisans(Pageable pageable);
}
