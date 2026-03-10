package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.TopProductDTO;
import org.example.catholicsouvenircustomorder.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
    SELECT
        p.productId,
        p.productName,
        SUM(od.quantity) AS sold,
        SUM(od.quantity * od.product.productPrice) AS revenue
    FROM OrderDetail od
    JOIN Product p ON p.productId = od.product.productId
    GROUP BY p.productId
    ORDER BY sold DESC
    LIMIT 10
""")
    List<TopProductDTO> getMostSoldProducts();
}
