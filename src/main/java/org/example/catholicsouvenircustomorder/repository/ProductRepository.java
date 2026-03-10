package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product,UUID> {
    @Query("""
   SELECT p FROM Product p
   WHERE p.artisan.artisanUuid = :artisanId
   AND (:status IS NULL OR p.status = :status)
""")
    Page<Product> findByArtisanIdWithOptionalStatus(
            @Param("artisanId") UUID artisanId,
            @Param("status") String status,
            Pageable pageable
    );
    @Query("""
    SELECT p.productId, p.productName, p.quantity
    FROM Product p
    WHERE p.quantity < 10
    ORDER BY p.quantity ASC
    """)
    List<Product> findShortStockProduct();
}
