package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.TopProductDTO;
import org.example.catholicsouvenircustomorder.model.OrderDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.TopProductDTO;
import java.util.List;
import java.util.UUID;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, UUID> {
    @Query("""
    SELECT od
    FROM OrderDetail od
    WHERE od.product.artisan.artisanUuid = :artisanId
""")
    List<OrderDetail> findOrderDetailsByArtisanId(UUID artisanId);

    @Query("""
    SELECT p.productId AS productId,
           p.productName AS productName,
           SUM(od.quantity) AS sold,
           SUM(od.quantity * p.productPrice) AS revenue
    FROM OrderDetail od
    JOIN od.product p
    WHERE p.artisan.artisanUuid = :artisanId
    GROUP BY p.productId, p.productName
    ORDER BY SUM(od.quantity * p.productPrice) DESC
""")
    List<TopProductDTO> getMostSoldProducts(UUID artisanId, Pageable pageable);

    @Query("""
    SELECT p.productId AS productId,
           p.productName AS productName,
           SUM(od.quantity) AS sold,
           SUM(od.quantity * p.productPrice * 0.05) AS revenue
    FROM OrderDetail od
    JOIN od.product p
    GROUP BY p.productId, p.productName
    ORDER BY SUM(od.quantity * p.productPrice) DESC
""")
    List<TopProductDTO> getAdminMostSoldProducts(Pageable pageable);
}
