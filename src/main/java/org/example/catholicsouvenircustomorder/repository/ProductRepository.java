package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.ShortStockProduct;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product,UUID>, JpaSpecificationExecutor<Product> {
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
    Optional<Product> findProductByProductIdAndArtisan_ArtisanUuid(UUID productId, UUID artisanUuid);
    @Query("""
    SELECT p.productId AS productId,
           p.productName AS productName,
           p.quantity AS quantity
    FROM Product p
    WHERE p.artisan.artisanUuid = :artisanId AND p.status <> 'DELETED'
    AND p.quantity <= 10
""")
    List<ShortStockProduct> findShortStockProduct(UUID artisanId);

    Page<Product> findProductByStatus(String status, Pageable pageable);
    @Query(value = """
    SELECT *,
           ts_rank(
               setweight(to_tsvector('simple', coalesce(product_name,'')), 'A') ||
               setweight(to_tsvector('simple', coalesce(product_description,'')), 'B'),
               plainto_tsquery('simple', :keyword)
           ) AS rank
    FROM product
     WHERE status <> 'DELETED'
               AND (
        setweight(to_tsvector('simple', coalesce(product_name,'')), 'A') ||
        setweight(to_tsvector('simple', coalesce(product_description,'')), 'B')
    ) @@ plainto_tsquery('simple', :keyword)
    ORDER BY rank DESC
""", nativeQuery = true)
    Page<Product> searchWithRanking(@Param("keyword") String keyword,Pageable pageable);
}
