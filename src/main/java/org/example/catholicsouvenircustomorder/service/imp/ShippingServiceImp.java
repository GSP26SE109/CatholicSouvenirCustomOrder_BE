package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.config.GHNConfig;
import org.example.catholicsouvenircustomorder.dto.request.CreateShipmentRequest;
import org.example.catholicsouvenircustomorder.dto.response.ShipmentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.repository.CustomOrderRepository;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.ShipmentRepository;
import org.example.catholicsouvenircustomorder.service.GHNAddressService;
import org.example.catholicsouvenircustomorder.service.ShippingService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingServiceImp implements ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ArtisanRepository artisanRepository;
    private final GHNAddressService ghnAddressService;

    @Override
    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        if (request.getOrderId() == null && request.getCustomOrderId() == null) {
            throw new BadRequestException("Phải cung cấp orderId hoặc customOrderId");
        }

        Order order = null;
        CustomOrder customOrder = null;

        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        }

        if (request.getCustomOrderId() != null) {
            customOrder = customOrderRepository.findById(request.getCustomOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng tùy chỉnh"));
        }

        Map<String, Object> ghnRequest = buildGHNRequest(request);
        Map<String, Object> ghnResponse = callGHNCreateOrder(ghnRequest);

        log.info("Parsing GHN response to create shipment entity");

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setCustomOrder(customOrder);
        shipment.setGhnOrderCode((String) ghnResponse.get("order_code"));
        shipment.setTrackingNumber((String) ghnResponse.get("order_code"));
        shipment.setStatus(ShippingStatus.PENDING);
        shipment.setDeliveryAddress(request.getDeliveryAddress());
        shipment.setRecipientName(request.getRecipientName());
        shipment.setRecipientPhone(request.getRecipientPhone());
        
        if (ghnResponse.get("total_fee") != null) {
            shipment.setShippingFee(new BigDecimal(ghnResponse.get("total_fee").toString()));
        }
        
        shipment.setNote(request.getNote());
        shipment.setEstimatedDelivery(LocalDateTime.now().plusDays(3));

        log.info("Saving shipment to database: {}", shipment.getGhnOrderCode());
        shipment = shipmentRepository.save(shipment);
        log.info("Shipment saved successfully with ID: {}", shipment.getShipmentId());

        return mapToResponse(shipment);
    }

    @Override
    public ShipmentResponse getShipmentByOrderId(UUID orderId) {
        Shipment shipment = shipmentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin vận chuyển"));
        return mapToResponse(shipment);
    }

    @Override
    public ShipmentResponse getShipmentByCustomOrderId(UUID customOrderId) {
        Shipment shipment = shipmentRepository.findByCustomOrderCustomOrderId(customOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin vận chuyển"));
        return mapToResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse trackShipment(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã vận đơn"));

        Map<String, Object> ghnStatus = callGHNTrackOrder(shipment.getGhnOrderCode());
        updateShipmentFromGHN(shipment, ghnStatus);

        shipment = shipmentRepository.save(shipment);
        return mapToResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse updateShipmentStatus(String ghnOrderCode) {
        Shipment shipment = shipmentRepository.findByGhnOrderCode(ghnOrderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vận chuyển"));

        Map<String, Object> ghnStatus = callGHNTrackOrder(ghnOrderCode);
        updateShipmentFromGHN(shipment, ghnStatus);

        shipment = shipmentRepository.save(shipment);
        return mapToResponse(shipment);
    }

    @Override
    public BigDecimal calculateShippingFee(CreateShipmentRequest request) {
        Map<String, Object> feeRequest = new HashMap<>();
        feeRequest.put("from_district_id", ghnConfig.getFromDistrictId());
        feeRequest.put("to_district_id", request.getToDistrictId());
        feeRequest.put("to_ward_code", request.getToWardCode());
        feeRequest.put("weight", request.getWeight());
        feeRequest.put("length", request.getLength());
        feeRequest.put("width", request.getWidth());
        feeRequest.put("height", request.getHeight());
        feeRequest.put("service_type_id", request.getServiceTypeId());
        feeRequest.put("insurance_value", request.getOrderValue());

        try {
            Map<String, Object> response = callGHNCalculateFee(feeRequest);
            return new BigDecimal(response.get("total").toString());
        } catch (Exception e) {
            log.error("Error calculating shipping fee: {}", e.getMessage());
            return BigDecimal.valueOf(30000);
        }
    }

    @Override
    @Transactional
    public void cancelShipment(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vận chuyển"));

        if (shipment.getStatus() != ShippingStatus.PENDING && 
            shipment.getStatus() != ShippingStatus.PICKING) {
            throw new BadRequestException("Không thể hủy đơn vận chuyển ở trạng thái hiện tại");
        }

        callGHNCancelOrder(shipment.getGhnOrderCode());
        shipment.setStatus(ShippingStatus.CANCELLED);
        shipmentRepository.save(shipment);
    }

    @Override
    @Transactional
    public void handleGHNWebhook(Map<String, Object> webhookData) {
        try {
            log.info("Processing GHN webhook: {}", webhookData);
            
            String orderCode = (String) webhookData.get("OrderCode");
            String status = (String) webhookData.get("Status");
            
            if (orderCode == null || status == null) {
                log.warn("Invalid webhook data: missing OrderCode or Status");
                return;
            }

            Shipment shipment = shipmentRepository.findByGhnOrderCode(orderCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vận chuyển với mã: " + orderCode));

            ShippingStatus newStatus = mapGHNStatus(status);
            shipment.setStatus(newStatus);

            if ("delivered".equalsIgnoreCase(status)) {
                shipment.setActualDelivery(LocalDateTime.now());
            }

            String history = shipment.getStatusHistory() != null ? shipment.getStatusHistory() : "";
            history += "\n" + LocalDateTime.now() + ": " + status;
            shipment.setStatusHistory(history);

            shipmentRepository.save(shipment);
            log.info("Updated shipment {} to status {}", orderCode, newStatus);

        } catch (Exception e) {
            log.error("Error processing GHN webhook: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> buildGHNRequest(CreateShipmentRequest request) {
        Map<String, Object> ghnRequest = new HashMap<>();

        // Get artisan info if this is a custom order
        String fromName = "Catholic Souvenir Shop";
        String fromPhone = "0901234567";
        
        if (request.getCustomOrderId() != null) {
            CustomOrder customOrder = customOrderRepository.findById(request.getCustomOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy custom order"));
            
            if (customOrder.getArtisan() != null) {
                fromName = customOrder.getArtisan().getArtisanName();
                // Get phone from artisan's account
                if (customOrder.getArtisan().getAccount() != null && 
                    customOrder.getArtisan().getAccount().getPhone() != null) {
                    fromPhone = customOrder.getArtisan().getAccount().getPhone();
                }
            }
        }
        
        ghnRequest.put("payment_type_id", request.getPaymentTypeId());
        ghnRequest.put("note", request.getNote());
        ghnRequest.put("required_note", "KHONGCHOXEMHANG");
        ghnRequest.put("from_name", fromName);
        ghnRequest.put("from_phone", fromPhone);
        ghnRequest.put("from_address", "123 Nguyen Hue");
        ghnRequest.put("from_ward_name", "Phường Bến Nghé");
        ghnRequest.put("from_district_name", "Quận 1");
        ghnRequest.put("from_province_name", "TP. Hồ Chí Minh");
        ghnRequest.put("to_name", request.getRecipientName());
        ghnRequest.put("to_phone", request.getRecipientPhone());
        ghnRequest.put("to_address", request.getDeliveryAddress());
        ghnRequest.put("to_ward_code", request.getToWardCode());
        ghnRequest.put("to_district_id", request.getToDistrictId());
        ghnRequest.put("cod_amount", request.getOrderValue());
        ghnRequest.put("content", "Catholic Souvenir Product");
        ghnRequest.put("weight", request.getWeight());
        ghnRequest.put("length", request.getLength());
        ghnRequest.put("width", request.getWidth());
        ghnRequest.put("height", request.getHeight());
        ghnRequest.put("service_type_id", request.getServiceTypeId());
        ghnRequest.put("insurance_value", request.getOrderValue());

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("name", "Catholic Souvenir");
        item.put("quantity", 1);
        item.put("weight", request.getWeight());
        items.add(item);
        ghnRequest.put("items", items);

        return ghnRequest;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callGHNCreateOrder(Map<String, Object> request) {
        try {
            String url = ghnConfig.getApiUrl() + "/shiip/public-api/v2/shipping-order/create";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());
            headers.set("ShopId", ghnConfig.getShopId().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            log.info("Calling GHN API: {} with request: {}", url, request);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            log.info("GHN API response: {}", body);
            
            if (body == null) {
                throw new BadRequestException("GHN API trả về response rỗng");
            }
            
            Integer code = body.get("code") != null ? Integer.parseInt(body.get("code").toString()) : null;
            
            if (code != null && code == 200) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data == null) {
                    throw new BadRequestException("GHN API không trả về dữ liệu");
                }
                return data;
            }

            String errorMessage = body.get("message") != null ? body.get("message").toString() : "Unknown error";
            throw new BadRequestException("GHN API error: " + errorMessage);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling GHN create order API: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể tạo đơn vận chuyển: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callGHNTrackOrder(String orderCode) {
        try {
            String url = ghnConfig.getApiUrl() + "/shiip/public-api/v2/shipping-order/detail";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("order_code", orderCode);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            
            if (body == null) {
                throw new BadRequestException("GHN API trả về response rỗng");
            }
            
            Integer code = body.get("code") != null ? Integer.parseInt(body.get("code").toString()) : null;
            
            if (code != null && code == 200) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data == null) {
                    throw new BadRequestException("GHN API không trả về dữ liệu");
                }
                return data;
            }

            String errorMessage = body.get("message") != null ? body.get("message").toString() : "Unknown error";
            throw new BadRequestException("GHN API error: " + errorMessage);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling GHN track order API: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể tra cứu đơn vận chuyển: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callGHNCalculateFee(Map<String, Object> request) {
        try {
            String url = ghnConfig.getApiUrl() + "/shiip/public-api/v2/shipping-order/fee";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());
            headers.set("ShopId", ghnConfig.getShopId().toString());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            log.info("Calling GHN calculate fee API: {} with request: {}", url, request);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            log.info("GHN calculate fee response: {}", body);
            
            if (body == null) {
                throw new BadRequestException("GHN API trả về response rỗng");
            }
            
            Integer code = body.get("code") != null ? Integer.parseInt(body.get("code").toString()) : null;
            
            if (code != null && code == 200) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data == null) {
                    throw new BadRequestException("GHN API không trả về dữ liệu");
                }
                return data;
            }

            String errorMessage = body.get("message") != null ? body.get("message").toString() : "Unknown error";
            throw new BadRequestException("GHN API error: " + errorMessage);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling GHN calculate fee API: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể tính phí vận chuyển: " + e.getMessage());
        }
    }

    private void callGHNCancelOrder(String orderCode) {
        try {
            String url = ghnConfig.getApiUrl() + "/shiip/public-api/v2/switch-status/cancel";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("order_codes", List.of(orderCode));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

        } catch (Exception e) {
            log.error("Error calling GHN cancel order API: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể hủy đơn vận chuyển: " + e.getMessage());
        }
    }

    private void updateShipmentFromGHN(Shipment shipment, Map<String, Object> ghnStatus) {
        String status = (String) ghnStatus.get("status");
        shipment.setStatus(mapGHNStatus(status));

        if (ghnStatus.get("finish_date") != null) {
            shipment.setActualDelivery(LocalDateTime.parse((String) ghnStatus.get("finish_date")));
        }

        String history = shipment.getStatusHistory() != null ? shipment.getStatusHistory() : "";
        history += "\n" + LocalDateTime.now() + ": " + status;
        shipment.setStatusHistory(history);
    }

    private ShippingStatus mapGHNStatus(String status) {
        return switch (status) {
            case "ready_to_pick" -> ShippingStatus.PENDING;
            case "picking" -> ShippingStatus.PICKING;
            case "picked" -> ShippingStatus.PICKED;
            case "storing" -> ShippingStatus.STORING;
            case "transporting" -> ShippingStatus.TRANSPORTING;
            case "delivering" -> ShippingStatus.DELIVERING;
            case "delivered" -> ShippingStatus.DELIVERED;
            case "return" -> ShippingStatus.RETURNED;
            case "cancel" -> ShippingStatus.CANCELLED;
            default -> ShippingStatus.PENDING;
        };
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {
        return ShipmentResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .orderId(shipment.getOrder() != null ? shipment.getOrder().getOrderId() : null)
                .customOrderId(shipment.getCustomOrder() != null ? shipment.getCustomOrder().getCustomOrderId() : null)
                .ghnOrderCode(shipment.getGhnOrderCode())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus())
                .pickAddress(shipment.getPickAddress())
                .deliveryAddress(shipment.getDeliveryAddress())
                .recipientName(shipment.getRecipientName())
                .recipientPhone(shipment.getRecipientPhone())
                .shippingFee(shipment.getShippingFee())
                .insuranceFee(shipment.getInsuranceFee())
                .estimatedDelivery(shipment.getEstimatedDelivery())
                .actualDelivery(shipment.getActualDelivery())
                .note(shipment.getNote())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
    
    // ==================== GHN Master Data APIs ====================
    
    @Override
    public List<Map<String, Object>> getProvinces() {
        return ghnAddressService.getProvinces();
    }
    
    @Override
    public List<Map<String, Object>> getDistricts(Integer provinceId) {
        return ghnAddressService.getDistricts(provinceId);
    }
    
    @Override
    public List<Map<String, Object>> getWards(Integer districtId) {
        return ghnAddressService.getWards(districtId);
    }
    
    @Override
    public Map<String, Object> searchDistrict(Integer provinceId, String districtName) {
        return ghnAddressService.searchDistrict(provinceId, districtName);
    }
    
    @Override
    public Map<String, Object> searchWard(Integer districtId, String wardName) {
        return ghnAddressService.searchWard(districtId, wardName);
    }
}
