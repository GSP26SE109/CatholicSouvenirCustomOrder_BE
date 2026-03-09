package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.NotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.CustomOrderRepository;
import org.example.catholicsouvenircustomorder.repository.CustomOrderStageRepository;
import org.example.catholicsouvenircustomorder.service.CustomOrderStageService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOrderStageServiceImp implements CustomOrderStageService {
    
    private final CustomOrderStageRepository stageRepository;
    private final CustomOrderRepository customOrderRepository;
    private final NotificationService notificationService;
    
    @Override
    public CustomOrderStageResponse getStageById(UUID stageId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        return mapToResponse(stage);
    }
    
    @Override
    public List<CustomOrderStageResponse> getStagesByOrderId(UUID orderId) {
        List<CustomOrderStage> stages = stageRepository
                .findByCustomOrder_OrderIdOrderByStageOrderAsc(orderId);
        return stages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public CustomOrderStageResponse completeStage(UUID stageId, CompleteStageRequest request, UUID artisanId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        // Validate artisan ownership
        if (!stage.getCustomOrder().getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new BadRequestException("Bạn không có quyền hoàn thành giai đoạn này");
        }
        
        // Validate stage is paid
        if (stage.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Giai đoạn phải được thanh toán trước khi hoàn thành");
        }
        
        // Validate not already completed
        if (stage.getCompletedAt() != null) {
            throw new BadRequestException("Giai đoạn này đã được hoàn thành");
        }
        
        // Complete the stage
        stage.setCompletedAt(LocalDateTime.now());
        stage.setCompletionImageUrl(request.getCompletionImageUrl());
        stage = stageRepository.save(stage);
        
        // Store values needed for lambda
        final Integer currentStageOrder = stage.getStageOrder();
        final UUID currentStageId = stage.getStageId();
        final UUID orderIdForQuery = stage.getCustomOrder().getOrderId();
        
        // Get all stages to find next stage
        List<CustomOrderStage> allStages = stageRepository
                .findByCustomOrder_OrderIdOrderByStageOrderAsc(orderIdForQuery);
        
        // Find next stage
        CustomOrderStage nextStage = allStages.stream()
                .filter(s -> s.getStageOrder().equals(currentStageOrder + 1))
                .findFirst()
                .orElse(null);
        
        // Notify customer about completion and next stage
        UUID customerId = stage.getCustomOrder().getCustomer().getAccountId();
        
        if (nextStage != null) {
            notificationService.notifyCustomerOfStageCompletion(
                customerId,
                currentStageId,
                stage.getName(),
                nextStage.getStageId(),
                nextStage.getName(),
                nextStage.getAmount().longValue()
            );
            log.info("Giai đoạn {} đã hoàn thành. Giai đoạn {} hiện có thể thanh toán", 
                    currentStageOrder, nextStage.getStageOrder());
        } else {
            notificationService.notifyCustomerOfStageCompletion(
                customerId,
                currentStageId,
                stage.getName(),
                null,
                null,
                null
            );
            // Check if all stages are completed
            boolean allCompleted = allStages.stream()
                    .allMatch(s -> s.getCompletedAt() != null);
            
            if (allCompleted) {
                CustomOrder order = stage.getCustomOrder();
                order.setStatus(CustomOrderStatus.COMPLETED);
                customOrderRepository.save(order);
                log.info("Tất cả giai đoạn đã hoàn thành cho đơn hàng {}", order.getOrderId());
            }
        }
        
        return mapToResponse(stage);
    }
    
    @Override
    public boolean canPayStage(UUID stageId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        // Already paid
        if (stage.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return false;
        }
        
        // First stage can always be paid
        if (stage.getStageOrder() == 1) {
            return true;
        }
        
        // Check if previous stage is completed and paid
        List<CustomOrderStage> allStages = stageRepository
                .findByCustomOrder_OrderIdOrderByStageOrderAsc(stage.getCustomOrder().getOrderId());
        
        CustomOrderStage previousStage = allStages.stream()
                .filter(s -> s.getStageOrder().equals(stage.getStageOrder() - 1))
                .findFirst()
                .orElse(null);
        
        return previousStage != null && 
               previousStage.getCompletedAt() != null && 
               previousStage.getPaymentStatus() == PaymentStatus.SUCCESS;
    }
    
    private CustomOrderStageResponse mapToResponse(CustomOrderStage stage) {
        return CustomOrderStageResponse.builder()
                .stageId(stage.getStageId())
                .orderId(stage.getCustomOrder().getOrderId())
                .stageOrder(stage.getStageOrder())
                .stageName(stage.getName())
                .description(stage.getDescription())
                .amount(stage.getAmount())
                .percentage(stage.getPaymentPercentage())
                .paymentStatus(stage.getPaymentStatus())
                .dueDate(stage.getDueDate())
                .paidAt(stage.getPaidAt())
                .completedAt(stage.getCompletedAt())
                .completionImageUrl(stage.getCompletionImageUrl())
                .canPay(canPayStage(stage.getStageId()))
                .canComplete(stage.getPaymentStatus() == PaymentStatus.SUCCESS && stage.getCompletedAt() == null)
                .createdAt(stage.getCreatedAt())
                .build();
    }
}
