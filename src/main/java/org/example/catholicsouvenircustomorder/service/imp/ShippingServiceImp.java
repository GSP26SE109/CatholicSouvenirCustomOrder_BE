package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.config.GHNConfig;
import org.example.catholicsouvenircustomorder.dto.request.CreateShipmentRequest;
import org.example.catholicsouvenircustomorder.dto.response.ShipmentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.InsufficientBalanceException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.GHNAddressService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.RefundService;
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
    private final ComplaintRepository complaintRepository;
    private final AccountRepository accountRepository;
    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ArtisanRepository artisanRepository;
    private final GHNAddressService ghnAddressService;
    private final RefundService refundService;
    private final NotificationService notificationService;

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
                
                // Update Order or CustomOrder status to DELIVERED
                if (shipment.getOrder() != null && !shipment.getIsReturn()) {
                    Order order = shipment.getOrder();
                    order.setStatus(String.valueOf(OrderStatus.DELIVERED));
                    order.setUpdateAt(LocalDateTime.now());
                    orderRepository.save(order);
                    log.info("Updated Order {} status to DELIVERED", order.getOrderId());
                    
                    // Send notification to customer
                    notificationService.sendNotification(
                        order.getCustomer().getAccountId(),
                        NotificationType.ORDER_DELIVERED,
                        "Đơn hàng đã được giao",
                        "Đơn hàng #" + order.getOrderId() + " đã được giao thành công. Bạn có thể đánh giá sản phẩm ngay bây giờ!",
                        order.getOrderId()
                    );
                    
                } else if (shipment.getCustomOrder() != null && !shipment.getIsReturn()) {
                    CustomOrder customOrder = shipment.getCustomOrder();
                    customOrder.setStatus(CustomOrderStatus.DELIVERED);
                    customOrder.setUpdatedAt(LocalDateTime.now());
                    customOrderRepository.save(customOrder);
                    log.info("Updated CustomOrder {} status to DELIVERED", customOrder.getCustomOrderId());
                    
                    // Send notification to customer
                    notificationService.sendNotification(
                        customOrder.getRequest().getCustomer().getAccountId(),
                        NotificationType.ORDER_DELIVERED,
                        "Đơn hàng tùy chỉnh đã được giao",
                        "Đơn hàng tùy chỉnh #" + customOrder.getCustomOrderId() + " đã được giao thành công. Bạn có thể đánh giá sản phẩm ngay bây giờ!",
                        customOrder.getCustomOrderId()
                    );
                }
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
    
    // ==================== Return Shipment Methods ====================
    
    /**
     * Customer creates return shipment for complaint
     * Requirements: 5.1, 5.2, 5.3
     */
    @Override
    @Transactional
    public ShipmentResponse createReturnShipment(UUID complaintId, CreateShipmentRequest request, UUID customerId) {
        log.info("Creating return shipment for complaint: {} by customer: {}", complaintId, customerId);
        
        // 1. Validate complaint exists and status is WAITING_RETURN
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại"));
        
        if (complaint.getStatus() != ComplaintStatus.WAITING_RETURN) {
            throw new BadRequestException("Khiếu nại không ở trạng thái chờ trả hàng");
        }
        
        // 2. Validate complaint belongs to customer
        if (!complaint.getCustomer().getAccountId().equals(customerId)) {
            throw new BadRequestException("Khiếu nại không thuộc về khách hàng này");
        }
        
        // 3. Check if return shipment already exists
        Optional<Shipment> existingReturn = shipmentRepository.findByComplaintComplaintIdAndIsReturnTrue(complaintId);
        if (existingReturn.isPresent()) {
            throw new BadRequestException("Đơn trả hàng đã tồn tại cho khiếu nại này");
        }
        
        // 4. Build GHN request for return shipment
        Map<String, Object> ghnRequest = buildReturnShipmentGHNRequest(request, complaint);
        Map<String, Object> ghnResponse = callGHNCreateOrder(ghnRequest);
        
        log.info("Creating return shipment entity for complaint: {}", complaintId);
        
        // 5. Create Shipment with isReturn = true and complaint reference
        Shipment shipment = new Shipment();
        shipment.setIsReturn(true);
        shipment.setComplaint(complaint);
        shipment.setGhnOrderCode((String) ghnResponse.get("order_code"));
        shipment.setTrackingNumber((String) ghnResponse.get("order_code"));
        shipment.setStatus(ShippingStatus.PENDING);
        shipment.setDeliveryAddress(request.getDeliveryAddress());
        shipment.setRecipientName(request.getRecipientName());
        shipment.setRecipientPhone(request.getRecipientPhone());
        
        if (ghnResponse.get("total_fee") != null) {
            shipment.setShippingFee(new BigDecimal(ghnResponse.get("total_fee").toString()));
        }
        
        shipment.setNote("Return shipment for complaint #" + complaintId);
        shipment.setEstimatedDelivery(LocalDateTime.now().plusDays(3));
        
        log.info("Saving return shipment to database: {}", shipment.getGhnOrderCode());
        shipment = shipmentRepository.save(shipment);
        log.info("Return shipment saved successfully with ID: {}", shipment.getShipmentId());
        
        return mapToResponse(shipment);
    }
    
    /**
     * Artisan confirms receipt of returned item
     * Requirements: 5.4, 5.5, 5.6
     */
    @Override
    @Transactional
    public ShipmentResponse confirmReturnReceipt(UUID shipmentId, UUID artisanId) {
        log.info("Artisan {} confirming return receipt for shipment: {}", artisanId, shipmentId);
        
        // 1. Validate shipment exists and isReturn = true
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vận chuyển"));
        
        if (!shipment.getIsReturn()) {
            throw new BadRequestException("Đơn vận chuyển này không phải là đơn trả hàng");
        }
        
        // 2. Validate shipment belongs to artisan (via complaint)
        if (shipment.getComplaint() == null) {
            throw new BadRequestException("Đơn trả hàng không có khiếu nại liên kết");
        }
        
        if (!shipment.getComplaint().getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new BadRequestException("Đơn trả hàng không thuộc về nghệ nhân này");
        }
        
        // 3. Update shipment status
        shipment.setStatus(ShippingStatus.DELIVERED);
        shipment.setActualDelivery(LocalDateTime.now());
        shipment = shipmentRepository.save(shipment);
        
        log.info("Return shipment confirmed: {}", shipmentId);
        
        // 4. Update complaint status from WAITING_RETURN to PROCESSING_REFUND
        Complaint complaint = shipment.getComplaint();
        complaint.setStatus(ComplaintStatus.PROCESSING_REFUND);
        complaintRepository.save(complaint);
        
        log.info("Complaint status updated to PROCESSING_REFUND: {}", complaint.getComplaintId());
        
        // 5. Process refund
        try {
            RefundTransaction refundTransaction = refundService.processRefund(
                complaint, 
                complaint.getRefundAmount()
            );
            
            // 6. Update complaint status to APPROVED after successful refund
            complaint.setStatus(ComplaintStatus.APPROVED);
            complaintRepository.save(complaint);
            
            log.info("Refund processed successfully for complaint: {}", complaint.getComplaintId());
            
            // Send success notifications
            notificationService.sendNotification(
                complaint.getCustomer().getAccountId(),
                NotificationType.REFUND_COMPLETED,
                "Hoàn tiền thành công",
                "Số tiền " + complaint.getRefundAmount() + " VND đã được hoàn vào ví của bạn.",
                complaint.getComplaintId()
            );
            
            notificationService.sendNotification(
                complaint.getArtisan().getAccount().getAccountId(),
                NotificationType.COMPLAINT_APPROVED,
                "Đã xác nhận nhận hàng trả về",
                "Bạn đã xác nhận nhận hàng trả về và số tiền " + complaint.getRefundAmount() + " VND đã được hoàn cho khách hàng.",
                complaint.getComplaintId()
            );
        } catch (InsufficientBalanceException e) {
            // Refund failed due to insufficient balance
            complaint.setStatus(ComplaintStatus.WAITING_RETURN);
            complaintRepository.save(complaint);
            
            log.error("Refund failed for complaint {}: {}", complaint.getComplaintId(), e.getMessage());
            
            // Send notification to admin
            Account platformAdmin = accountRepository.findByRole_Name("ADMIN").stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Admin không tồn tại"));
            
            notificationService.sendNotification(
                platformAdmin.getAccountId(),
                NotificationType.REFUND_FAILED,
                "Hoàn tiền thất bại",
                "Hoàn tiền cho khiếu nại #" + complaint.getComplaintId() + " thất bại: " + e.getMessage(),
                complaint.getComplaintId()
            );
            
            throw e;
        }
        
        return mapToResponse(shipment);
    }
    
    /**
     * Get return shipment for complaint
     * Requirements: 5.6
     */
    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getReturnShipmentByComplaint(UUID complaintId) {
        log.info("Getting return shipment for complaint: {}", complaintId);
        
        Shipment shipment = shipmentRepository.findByComplaintComplaintIdAndIsReturnTrue(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn trả hàng cho khiếu nại này"));
        
        return mapToResponse(shipment);
    }
    
    /**
     * Build GHN request for return shipment
     * Customer sends item back to Artisan
     */
    private Map<String, Object> buildReturnShipmentGHNRequest(CreateShipmentRequest request, Complaint complaint) {
        Map<String, Object> ghnRequest = new HashMap<>();
        
        // For return shipment: Customer is sender, Artisan is receiver
        Account customer = complaint.getCustomer();
        Artisan artisan = complaint.getArtisan();
        
        // From: Customer
        String fromName = customer.getFullName();
        String fromPhone = customer.getPhone() != null ? customer.getPhone() : "0901234567";
        
        // To: Artisan
        String toName = artisan.getArtisanName();
        String toPhone = artisan.getAccount().getPhone() != null ? artisan.getAccount().getPhone() : "0901234567";
        
        ghnRequest.put("payment_type_id", request.getPaymentTypeId());
        ghnRequest.put("note", "Return shipment for complaint #" + complaint.getComplaintId());
        ghnRequest.put("required_note", "KHONGCHOXEMHANG");
        ghnRequest.put("from_name", fromName);
        ghnRequest.put("from_phone", fromPhone);
        ghnRequest.put("from_address", "Customer Address"); // Customer's address
        ghnRequest.put("from_ward_name", "Phường 1");
        ghnRequest.put("from_district_name", "Quận 1");
        ghnRequest.put("from_province_name", "TP. Hồ Chí Minh");
        ghnRequest.put("to_name", toName);
        ghnRequest.put("to_phone", toPhone);
        ghnRequest.put("to_address", request.getDeliveryAddress());
        ghnRequest.put("to_ward_code", request.getToWardCode());
        ghnRequest.put("to_district_id", request.getToDistrictId());
        ghnRequest.put("cod_amount", 0); // No COD for return shipment
        ghnRequest.put("content", "Return Item - Catholic Souvenir");
        ghnRequest.put("weight", request.getWeight());
        ghnRequest.put("length", request.getLength());
        ghnRequest.put("width", request.getWidth());
        ghnRequest.put("height", request.getHeight());
        ghnRequest.put("service_type_id", request.getServiceTypeId());
        ghnRequest.put("insurance_value", 0);
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("name", "Return Item");
        item.put("quantity", 1);
        item.put("weight", request.getWeight());
        items.add(item);
        ghnRequest.put("items", items);
        
        return ghnRequest;
    }
    
    /**
     * Get shipping timeline for FE display
     */
    @Override
    @Transactional(readOnly = true)
    public org.example.catholicsouvenircustomorder.dto.response.ShippingTimelineResponse getShippingTimeline(UUID shipmentId) {
        log.info("Getting shipping timeline for shipment: {}", shipmentId);
        
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vận chuyển"));
        
        ShippingStatus currentStatus = shipment.getStatus();
        
        // Define all possible steps in order
        List<org.example.catholicsouvenircustomorder.dto.response.ShippingTimelineResponse.TimelineStep> timeline = new ArrayList<>();
        
        // Parse status history to get completion times
        Map<ShippingStatus, LocalDateTime> statusTimes = parseStatusHistory(shipment.getStatusHistory());
        
        // Build timeline steps
        addTimelineStep(timeline, ShippingStatus.PENDING, "Chờ lấy hàng", 
                "Đơn hàng đã được tạo, chờ shipper đến lấy", currentStatus, statusTimes);
        addTimelineStep(timeline, ShippingStatus.PICKING, "Đang lấy hàng", 
                "Shipper đang trên đường đến lấy hàng", currentStatus, statusTimes);
        addTimelineStep(timeline, ShippingStatus.PICKED, "Đã lấy hàng", 
                "Đã lấy hàng từ người gửi", currentStatus, statusTimes);
        addTimelineStep(timeline, ShippingStatus.STORING, "Nhập kho", 
                "Hàng đang được lưu tại kho trung chuyển", currentStatus, statusTimes);
        addTimelineStep(timeline, ShippingStatus.TRANSPORTING, "Đang vận chuyển", 
                "Hàng đang được vận chuyển đến kho đích", currentStatus, statusTimes);
        addTimelineStep(timeline, ShippingStatus.DELIVERING, "Đang giao hàng", 
                "Shipper đang giao hàng đến người nhận", currentStatus, statusTimes);
        addTimelineStep(timeline, ShippingStatus.DELIVERED, "Đã giao hàng", 
                "Giao hàng thành công", currentStatus, statusTimes);
        
        return org.example.catholicsouvenircustomorder.dto.response.ShippingTimelineResponse.builder()
                .currentStatus(currentStatus)
                .currentStatusLabel(getStatusLabel(currentStatus))
                .currentStatusDescription(getStatusDescription(currentStatus))
                .timeline(timeline)
                .statusHistory(shipment.getStatusHistory())
                .build();
    }
    
    private void addTimelineStep(
            List<org.example.catholicsouvenircustomorder.dto.response.ShippingTimelineResponse.TimelineStep> timeline,
            ShippingStatus status,
            String label,
            String description,
            ShippingStatus currentStatus,
            Map<ShippingStatus, LocalDateTime> statusTimes) {
        
        int currentOrder = getStatusOrder(currentStatus);
        int stepOrder = getStatusOrder(status);
        
        timeline.add(org.example.catholicsouvenircustomorder.dto.response.ShippingTimelineResponse.TimelineStep.builder()
                .status(status)
                .label(label)
                .description(description)
                .completed(stepOrder < currentOrder)
                .current(status == currentStatus)
                .completedAt(statusTimes.get(status))
                .build());
    }
    
    private int getStatusOrder(ShippingStatus status) {
        return switch (status) {
            case PENDING -> 1;
            case PICKING -> 2;
            case PICKED -> 3;
            case STORING -> 4;
            case TRANSPORTING -> 5;
            case DELIVERING -> 6;
            case DELIVERED -> 7;
            case RETURNED, CANCELLED -> 99; // Special cases
        };
    }
    
    private String getStatusLabel(ShippingStatus status) {
        return switch (status) {
            case PENDING -> "Chờ lấy hàng";
            case PICKING -> "Đang lấy hàng";
            case PICKED -> "Đã lấy hàng";
            case STORING -> "Nhập kho";
            case TRANSPORTING -> "Đang vận chuyển";
            case DELIVERING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao hàng";
            case RETURNED -> "Hoàn trả";
            case CANCELLED -> "Đã hủy";
        };
    }
    
    private String getStatusDescription(ShippingStatus status) {
        return switch (status) {
            case PENDING -> "Đơn hàng đã được tạo, chờ shipper đến lấy";
            case PICKING -> "Shipper đang trên đường đến lấy hàng";
            case PICKED -> "Đã lấy hàng từ người gửi";
            case STORING -> "Hàng đang được lưu tại kho trung chuyển";
            case TRANSPORTING -> "Hàng đang được vận chuyển đến kho đích";
            case DELIVERING -> "Shipper đang giao hàng đến người nhận";
            case DELIVERED -> "Giao hàng thành công";
            case RETURNED -> "Hàng bị trả lại";
            case CANCELLED -> "Đơn hàng đã bị hủy";
        };
    }
    
    private Map<ShippingStatus, LocalDateTime> parseStatusHistory(String statusHistory) {
        Map<ShippingStatus, LocalDateTime> result = new HashMap<>();
        if (statusHistory == null || statusHistory.isEmpty()) {
            return result;
        }
        
        // Parse format: "2024-01-15T10:30:00: picking\n2024-01-15T11:00:00: picked"
        String[] lines = statusHistory.split("\n");
        for (String line : lines) {
            try {
                String[] parts = line.split(": ", 2);
                if (parts.length == 2) {
                    LocalDateTime time = LocalDateTime.parse(parts[0].trim());
                    ShippingStatus status = mapGHNStatus(parts[1].trim());
                    result.put(status, time);
                }
            } catch (Exception e) {
                log.warn("Failed to parse status history line: {}", line);
            }
        }
        
        return result;
    }
}

