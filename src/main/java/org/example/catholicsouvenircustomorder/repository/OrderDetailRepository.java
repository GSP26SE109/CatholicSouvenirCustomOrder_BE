package org.example.catholicsouvenircustomorder.repository;

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
}
