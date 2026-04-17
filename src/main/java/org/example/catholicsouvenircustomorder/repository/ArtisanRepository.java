package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtisanRepository extends JpaRepository<Artisan, UUID> {
    boolean existsByAccount(Account account);
    
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
}
