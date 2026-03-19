package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateCustomOrderRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.CustomOrderService;
import org.example.catholicsouvenircustomorder.service.CustomOrderStageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOrderServiceImp implements CustomOrderService {

    private final CustomOrderRepository customOrderRepository;
    private final CustomRequestRepository customRequestRepository;
    private final QuotationRepository quotationRepository;
    private final ArtisanRepository artisanRepository;
    private final CustomOrderStageRepository stageRepository;
    private final CustomOrderStageService stageService;

    @Override
    @Transactional
    public CustomOrderResponse createCustomOrder(CreateCustomOrderRequest request, UUID artisanId) {
        // Validate and get entities
        CustomRequest customRequest = getCustomRequest(request.getRequestId());
        Quotation quotation = getQuotation(request.getQuotationId());
        Artisan artisan = getArtisan(artisanId);
        
        // Validate business rules
        validateQuotationOwnership(quotation, artisanId);
        validateArtisanConfirmation(customRequest, artisanId);
        validatePaymentPercentages(request.getStages());
        
        // Create order
        CustomOrder order = buildCustomOrder(customRequest, quotation, artisan, request.getShippingAddress());
        order = customOrderRepository.save(order);
        
        // Create stages
        List<CustomOrderStage> stages = createStages(order, request.getStages(), quotation.getPrice());
        order.setStages(stages);
        
        // Update related entities
        updateCustomRequestStatus(customRequest);
        updateQuotationStatus(quotation);
        
        log.info("Created custom order {} with {} stages", order.getOrderId(), stages.size());
        return mapToResponse(order);
    }

    @Override
    public CustomOrderResponse getCustomOrderById(UUID orderId) {
        CustomOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng tùy chỉnh"));
        return mapToResponse(order);
    }

    @Override
    public List<CustomOrderResponse> getCustomerOrders(UUID customerId) {
        return customOrderRepository.findByCustomer_AccountId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomOrderResponse> getArtisanOrders(UUID artisanId) {
        return customOrderRepository.findByArtisan_ArtisanUuid(artisanId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomOrderResponse completeStage(CompleteStageRequest request, UUID artisanId) {
        // Delegate to StageService for stage completion logic
        stageService.completeStage(request.getStageId(), request, artisanId);
        
        // Get updated order
        CustomOrderStage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giai đoạn"));
        
        return mapToResponse(stage.getCustomOrder());
    }

    @Override
    @Transactional
    public CustomOrderResponse updateOrderStatus(UUID orderId, String status) {
        CustomOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng tùy chỉnh"));

        order.setStatus(CustomOrderStatus.valueOf(status));
        order = customOrderRepository.save(order);

        log.info("Đã cập nhật trạng thái đơn hàng {} thành {}", orderId, status);
        return mapToResponse(order);
    }

    // ==================== Private Helper Methods ====================
    
    private CustomRequest getCustomRequest(UUID requestId) {
        return customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tùy chỉnh"));
    }
    
    private Quotation getQuotation(UUID quotationId) {
        return quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá"));
    }
    
    private Artisan getArtisan(UUID artisanId) {
        return artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));
    }
    
    private void validateQuotationOwnership(Quotation quotation, UUID artisanId) {
        if (!quotation.getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new BadRequestException("Báo giá này không thuộc về bạn");
        }
    }
    
    private void validateArtisanConfirmation(CustomRequest customRequest, UUID artisanId) {
        if (!customRequest.getConfirmedArtisan().getArtisanUuid().equals(artisanId)) {
            throw new BadRequestException("Bạn không phải là nghệ nhân được chọn cho yêu cầu này");
        }
    }
    
    private void validatePaymentPercentages(List<CreateCustomOrderRequest.StageDefinition> stages) {
        int totalPercentage = stages.stream()
                .mapToInt(CreateCustomOrderRequest.StageDefinition::getPaymentPercentage)
                .sum();
        
        if (totalPercentage != 100) {
            throw new BadRequestException("Tổng phần trăm thanh toán phải bằng 100%, hiện tại là " + totalPercentage + "%");
        }
    }
    
    private CustomOrder buildCustomOrder(CustomRequest customRequest, Quotation quotation, 
                                        Artisan artisan, String shippingAddress) {
        CustomOrder order = new CustomOrder();
        order.setCustomRequest(customRequest);
        order.setCustomer(customRequest.getCustomer());
        order.setArtisan(artisan);
        order.setFinalQuotation(quotation);
        order.setTotalAmount(quotation.getPrice());
        order.setShippingAddress(shippingAddress);
        order.setStatus(CustomOrderStatus.PENDING);
        return order;
    }
    
    private List<CustomOrderStage> createStages(CustomOrder order, 
                                               List<CreateCustomOrderRequest.StageDefinition> stageDefs,
                                               BigDecimal totalPrice) {
        List<CustomOrderStage> stages = new ArrayList<>();
        
        for (int i = 0; i < stageDefs.size(); i++) {
            CreateCustomOrderRequest.StageDefinition stageDef = stageDefs.get(i);
            CustomOrderStage stage = buildStage(order, stageDef, i + 1, totalPrice);
            stages.add(stage);
        }
        
        return stageRepository.saveAll(stages);
    }
    
    private CustomOrderStage buildStage(CustomOrder order, 
                                       CreateCustomOrderRequest.StageDefinition stageDef,
                                       int stageOrder,
                                       BigDecimal totalPrice) {
        CustomOrderStage stage = new CustomOrderStage();
        stage.setCustomOrder(order);
        stage.setName(stageDef.getName());
        stage.setDescription(stageDef.getDescription());
        stage.setStageOrder(stageOrder);
        stage.setPaymentPercentage(stageDef.getPaymentPercentage());
        
        BigDecimal stageAmount = totalPrice
                .multiply(BigDecimal.valueOf(stageDef.getPaymentPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        stage.setAmount(stageAmount);
        stage.setPaymentStatus(PaymentStatus.PENDING);
        
        return stage;
    }
    
    private void updateCustomRequestStatus(CustomRequest customRequest) {
        customRequest.setStatus(CustomRequestStatus.ORDER_CREATED);
        customRequestRepository.save(customRequest);
    }
    
    private void updateQuotationStatus(Quotation quotation) {
        quotation.setStatus(QuotationStatus.ACCEPTED);
        quotationRepository.save(quotation);
    }
    
    // ==================== Mapping Methods ====================
    
    private CustomOrderResponse mapToResponse(CustomOrder order) {
        CustomOrderResponse response = new CustomOrderResponse();
        response.setOrderId(order.getOrderId());
        response.setRequestId(order.getCustomRequest().getRequestId());
        response.setCustomerId(order.getCustomer().getAccountId());
        response.setCustomerName(order.getCustomer().getFullName());
        response.setArtisanId(order.getArtisan().getArtisanUuid());
        response.setArtisanName(order.getArtisan().getArtisanName());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        if (order.getStages() != null && !order.getStages().isEmpty()) {
            response.setStages(order.getStages().stream()
                    .map(this::mapToStageInfo)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private CustomOrderResponse.StageInfo mapToStageInfo(CustomOrderStage stage) {
        CustomOrderResponse.StageInfo info = new CustomOrderResponse.StageInfo();
        info.setStageId(stage.getStageId());
        info.setName(stage.getName());
        info.setDescription(stage.getDescription());
        info.setStageOrder(stage.getStageOrder());
        info.setPaymentPercentage(stage.getPaymentPercentage());
        info.setAmount(stage.getAmount());
        info.setStatus(stage.getPaymentStatus().name());
        info.setCompletionImageUrl(stage.getCompletionImageUrl());
        info.setCompletedAt(stage.getCompletedAt());
        info.setPaidAt(stage.getPaidAt());
        
        // Calculate canPay: 
        // - Stage 1: Always can pay (deposit)
        // - Stage 2+: Can pay only if previous stage is completed AND paid
        boolean canPay = stage.getPaymentStatus() == PaymentStatus.PENDING;
        if (canPay && stage.getStageOrder() > 1) {
            List<CustomOrderStage> allStages = stage.getCustomOrder().getStages();
            CustomOrderStage previousStage = allStages.stream()
                    .filter(s -> s.getStageOrder().equals(stage.getStageOrder() - 1))
                    .findFirst()
                    .orElse(null);
            
            canPay = previousStage != null && 
                     previousStage.getCompletedAt() != null && 
                     previousStage.getPaymentStatus() == PaymentStatus.SUCCESS;
        }
        info.setCanPay(canPay);
        
        return info;
    }
}
