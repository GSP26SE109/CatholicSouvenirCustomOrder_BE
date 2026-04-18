package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateShipmentRequest;
import org.example.catholicsouvenircustomorder.dto.response.ShipmentResponse;
import org.example.catholicsouvenircustomorder.dto.response.ShippingTimelineResponse;
import org.example.catholicsouvenircustomorder.service.GHNAddressService;
import org.example.catholicsouvenircustomorder.service.ShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;
    private final GHNAddressService ghnAddressService;

    @PostMapping("/shipments")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse response = shippingService.createShipment(request);
        return ResponseEntity.ok(BaseResponse.success("Tạo đơn vận chuyển thành công", response));
    }

    @GetMapping("/shipments/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getShipmentByOrderId(@PathVariable UUID orderId) {
        ShipmentResponse response = shippingService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin vận chuyển thành công", response));
    }

    @GetMapping("/shipments/custom-order/{customOrderId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getShipmentByCustomOrderId(@PathVariable UUID customOrderId) {
        ShipmentResponse response = shippingService.getShipmentByCustomOrderId(customOrderId);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin vận chuyển thành công", response));
    }

    @GetMapping("/shipments/track/{trackingNumber}")
    public ResponseEntity<BaseResponse> trackShipment(@PathVariable String trackingNumber) {
        ShipmentResponse response = shippingService.trackShipment(trackingNumber);
        return ResponseEntity.ok(BaseResponse.success("Tra cứu vận đơn thành công", response));
    }

    @PostMapping("/shipments/calculate-fee")
    public ResponseEntity<BaseResponse> calculateShippingFee(
            @Valid @RequestBody CreateShipmentRequest request) {
        BigDecimal fee = shippingService.calculateShippingFee(request);
        return ResponseEntity.ok(BaseResponse.success("Tính phí vận chuyển thành công", fee));
    }

    @PostMapping("/shipments/{shipmentId}/cancel")
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> cancelShipment(@PathVariable UUID shipmentId) {
        shippingService.cancelShipment(shipmentId);
        return ResponseEntity.ok(BaseResponse.success("Hủy đơn vận chuyển thành công"));
    }

    @PostMapping("/webhook/ghn")
    public ResponseEntity<BaseResponse> ghnWebhook(@RequestBody Map<String, Object> webhookData) {
        log.info("Received GHN webhook: {}", webhookData);
        shippingService.handleGHNWebhook(webhookData);
        return ResponseEntity.ok(BaseResponse.success("Webhook processed successfully"));
    }
    
    /**
     * DEMO ONLY: Simulate GHN webhook for testing
     * This endpoint allows you to manually trigger shipment status updates
     * 
     * Usage:
     * POST /api/shipments/demo/update-status
     * {
     *   "orderCode": "GHN_ORDER_CODE",
     *   "status": "ready_to_pick|picking|picked|storing|transporting|delivering|delivered|return"
     * }
     */
    @PostMapping("/shipments/demo/update-status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> simulateGHNWebhook(@RequestBody Map<String, Object> request) {
        String orderCode = (String) request.get("orderCode");
        String status = (String) request.get("status");
        
        if (orderCode == null || status == null) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error(400, "orderCode and status are required"));
        }
        
        // Simulate GHN webhook payload
        Map<String, Object> webhookData = Map.of(
            "OrderCode", orderCode,
            "Status", status,
            "Description", "Demo: Manual status update",
            "Time", System.currentTimeMillis() / 1000
        );
        
        log.info("DEMO: Simulating GHN webhook for order: {}, status: {}", orderCode, status);
        shippingService.handleGHNWebhook(webhookData);
        
        return ResponseEntity.ok(BaseResponse.success(
            "Demo webhook processed successfully. Order " + orderCode + " updated to " + status
        ));
    }
    
    /**
     * Get shipping timeline for frontend display
     * GET /api/shipments/{shipmentId}/timeline
     */
    @GetMapping("/shipments/{shipmentId}/timeline")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getShippingTimeline(@PathVariable UUID shipmentId) {
        ShippingTimelineResponse timeline = shippingService.getShippingTimeline(shipmentId);
        return ResponseEntity.ok(BaseResponse.success("Lấy timeline vận chuyển thành công", timeline));
    }
    
    /**
     * Get all available shipping statuses for demo/testing
     * GET /api/shipments/demo/statuses
     */
    @GetMapping("/shipments/demo/statuses")
    public ResponseEntity<BaseResponse> getAvailableStatuses() {
        List<Map<String, String>> statuses = List.of(
            Map.of("value", "ready_to_pick", "label", "Sẵn sàng lấy hàng", "description", "Đơn hàng đã được tạo, chờ shipper đến lấy"),
            Map.of("value", "picking", "label", "Đang lấy hàng", "description", "Shipper đang trên đường đến lấy hàng"),
            Map.of("value", "picked", "label", "Đã lấy hàng", "description", "Đã lấy hàng từ người gửi"),
            Map.of("value", "storing", "label", "Nhập kho", "description", "Hàng đang được lưu tại kho trung chuyển"),
            Map.of("value", "transporting", "label", "Đang vận chuyển", "description", "Hàng đang được vận chuyển đến kho đích"),
            Map.of("value", "delivering", "label", "Đang giao hàng", "description", "Shipper đang giao hàng đến người nhận"),
            Map.of("value", "delivered", "label", "Đã giao hàng", "description", "Giao hàng thành công"),
            Map.of("value", "return", "label", "Hoàn trả", "description", "Hàng bị trả lại"),
            Map.of("value", "cancel", "label", "Đã hủy", "description", "Đơn hàng đã bị hủy")
        );
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách trạng thái thành công", statuses));
    }
    
    @GetMapping("/shipments/my-shipments")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<BaseResponse> getMyShipments(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UUID customerId) {
        // TODO: Implement getCustomerShipments in ShippingService
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách vận đơn thành công"));
    }
    
    // ==================== GHN Master Data APIs ====================
    
    /**
     * Lấy danh sách tỉnh/thành phố
     */
    @GetMapping("/shipments/address/provinces")
    public ResponseEntity<BaseResponse> getProvinces() {
        List<Map<String, Object>> provinces = ghnAddressService.getProvinces();
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách tỉnh/thành phố thành công", provinces));
    }

    /**
     * Lấy danh sách quận/huyện theo tỉnh
     */
    @GetMapping("/shipments/address/districts")
    public ResponseEntity<BaseResponse> getDistricts(@RequestParam Integer provinceId) {
        List<Map<String, Object>> districts = ghnAddressService.getDistricts(provinceId);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách quận/huyện thành công", districts));
    }

    /**
     * Lấy danh sách phường/xã theo quận
     */
    @GetMapping("/shipments/address/wards")
    public ResponseEntity<BaseResponse> getWards(@RequestParam Integer districtId) {
        List<Map<String, Object>> wards = ghnAddressService.getWards(districtId);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách phường/xã thành công", wards));
    }

    /**
     * Tìm quận theo tên
     */
    @GetMapping("/shipments/address/districts/search")
    public ResponseEntity<BaseResponse> searchDistrict(
            @RequestParam Integer provinceId,
            @RequestParam String districtName) {
        Map<String, Object> district = ghnAddressService.searchDistrict(provinceId, districtName);
        return ResponseEntity.ok(BaseResponse.success("Tìm quận/huyện thành công", district));
    }

    /**
     * Tìm phường theo tên
     */
    @GetMapping("/shipments/address/wards/search")
    public ResponseEntity<BaseResponse> searchWard(
            @RequestParam Integer districtId,
            @RequestParam String wardName) {
        Map<String, Object> ward = ghnAddressService.searchWard(districtId, wardName);
        return ResponseEntity.ok(BaseResponse.success("Tìm phường/xã thành công", ward));
    }
}
