package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Shipment;
import org.example.catholicsouvenircustomorder.model.ShippingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByOrderOrderId(UUID orderId);
    Optional<Shipment> findByCustomOrderCustomOrderId(UUID customOrderId);
    Optional<Shipment> findByGhnOrderCode(String ghnOrderCode);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
}
