package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateShipmentRequest;
import org.example.catholicsouvenircustomorder.dto.response.ShipmentResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface ShippingService {
    ShipmentResponse createShipment(CreateShipmentRequest request);
    ShipmentResponse getShipmentByOrderId(UUID orderId);
    ShipmentResponse getShipmentByCustomOrderId(UUID customOrderId);
    ShipmentResponse trackShipment(String trackingNumber);
    ShipmentResponse updateShipmentStatus(String ghtkOrderLabel);
    BigDecimal calculateShippingFee(CreateShipmentRequest request);
    void cancelShipment(UUID shipmentId);
    void handleGHNWebhook(Map<String, Object> webhookData);
}
