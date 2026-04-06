package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateShipmentRequest;
import org.example.catholicsouvenircustomorder.dto.response.ShipmentResponse;
import org.example.catholicsouvenircustomorder.service.ShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping
    @PreAuthorize("hasAuthority('ARTISAN')")
    public ResponseEntity<BaseResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse response = shippingService.createShipment(request);
        return ResponseEntity.ok(BaseResponse.success("Tạo đơn vận chuyển thành công", response));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getShipmentByOrderId(@PathVariable UUID orderId) {
        ShipmentResponse response = shippingService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin vận chuyển thành công", response));
    }

    @GetMapping("/custom-order/{customOrderId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ARTISAN')")
    public ResponseEntity<BaseResponse> getShipmentByCustomOrderId(@PathVariable UUID customOrderId) {
        ShipmentResponse response = shippingService.getShipmentByCustomOrderId(customOrderId);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin vận chuyển thành công", response));
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<BaseResponse> trackShipment(@PathVariable String trackingNumber) {
        ShipmentResponse response = shippingService.trackShipment(trackingNumber);
        return ResponseEntity.ok(BaseResponse.success("Tra cứu vận đơn thành công", response));
    }

    @PostMapping("/calculate-fee")
    public ResponseEntity<BaseResponse> calculateShippingFee(
            @Valid @RequestBody CreateShipmentRequest request) {
        BigDecimal fee = shippingService.calculateShippingFee(request);
        return ResponseEntity.ok(BaseResponse.success("Tính phí vận chuyển thành công", fee));
    }

    @PostMapping("/{shipmentId}/cancel")
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
}
