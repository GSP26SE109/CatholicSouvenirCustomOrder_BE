package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateShipmentRequest;
import org.example.catholicsouvenircustomorder.dto.response.ShipmentResponse;
import org.example.catholicsouvenircustomorder.dto.response.ShippingTimelineResponse;

import java.math.BigDecimal;
import java.util.List;
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
    
    // Timeline for FE display
    ShippingTimelineResponse getShippingTimeline(UUID shipmentId);
    
    // GHN Master Data APIs
    List<Map<String, Object>> getProvinces();
    List<Map<String, Object>> getDistricts(Integer provinceId);
    List<Map<String, Object>> getWards(Integer districtId);
    Map<String, Object> searchDistrict(Integer provinceId, String districtName);
    Map<String, Object> searchWard(Integer districtId, String wardName);
    
    // Return Shipment Methods
    ShipmentResponse createReturnShipment(UUID complaintId, CreateShipmentRequest request, UUID customerId);
    ShipmentResponse confirmReturnReceipt(UUID shipmentId, UUID artisanId);
    ShipmentResponse getReturnShipmentByComplaint(UUID complaintId);
}
