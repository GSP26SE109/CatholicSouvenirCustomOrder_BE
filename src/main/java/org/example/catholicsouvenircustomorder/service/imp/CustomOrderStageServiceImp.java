package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.InitiateStagePaymentRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentInitiationResponse;
import org.example.catholicsouvenircustomorder.dto.response.StagePaymentResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.NotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.CustomOrderRepository;
import org.example.catholicsouvenircustomorder.repository.CustomOrderStageRepository;
import org.example.catholicsouvenircustomorder.service.CustomOrderStageService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.StagePaymentService;
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
    private final StagePaymentService paymentService;
    
    @Override
    public CustomOrderStageResponse getStageById(UUID stageId, UUID userId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        // Verify access
        CustomOrder order = stage.getCustomOrder();
        UUID customerId = order.getRequest().getCustomer().getAccountId();
        UUID artisanId = order.getArtisan() != null ? order.getArtisan().getArtisanUuid() : null;
        
        if (!userId.equals(customerId) && !userId.equals(artisanId)) {
            throw new BadRequestException("Bạn không có quyền xem giai đoạn này");
        }
        
        return mapToResponse(stage);
    }
    
    @Override
    @Transactional
    public CustomOrderStageResponse startStage(UUID stageId, UUID artisanId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        // Validate artisan ownership
        if (!stage.getCustomOrder().getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new BadRequestException("Bạn không có quyền bắt đầu giai đoạn này");
        }
        
        // Validate stage is paid
        if (!stage.getIsPaid()) {
            throw new BadRequestException("Giai đoạn phải được thanh toán trước khi bắt đầu");
        }
        
        // Validate stage status is PAID
        if (stage.getStatus() != StageStatus.PAID) {
            throw new BadRequestException("Giai đoạn phải ở trạng thái PAID để bắt đầu");
        }
        
        // Validate not already completed
        if (stage.getIsCompleted()) {
            throw new BadRequestException("Giai đoạn này đã được hoàn thành");
        }
        
        // Start the stage
        stage.setStatus(StageStatus.IN_PROGRESS);
        stage = stageRepository.save(stage);
        
        log.info("Artisan {} started stage {}", artisanId, stageId);
        
        // Send notification to customer
        UUID customerId = stage.getCustomOrder().getRequest().getCustomer().getAccountId();
        notificationService.sendNotification(
            customerId,
            NotificationType.STAGE_STARTED,
            "Nghệ nhân đã bắt đầu làm việc",
            String.format("Nghệ nhân đã bắt đầu thực hiện giai đoạn: %s", stage.getName()),
            stage.getCustomOrder().getCustomOrderId()
        );
        
        return mapToResponse(stage);
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
        if (!stage.getIsPaid()) {
            throw new BadRequestException("Giai đoạn phải được thanh toán trước khi hoàn thành");
        }
        
        // Validate not already completed
        if (stage.getIsCompleted()) {
            throw new BadRequestException("Giai đoạn này đã được hoàn thành");
        }
        
        // Complete the stage
        stage.setCompletedAt(LocalDateTime.now());
        stage.setCompletionImageUrl(request.getCompletionImageUrl());
        stage.setIsCompleted(true);  // ← Set workflow flag
        stage.setStatus(StageStatus.COMPLETED);
        stage = stageRepository.save(stage);
        
        // Store values needed for lambda
        final Integer currentStageOrder = stage.getStageOrder();
        final UUID currentStageId = stage.getStageId();
        final UUID orderIdForQuery = stage.getCustomOrder().getCustomOrderId();
        
        // Get all stages to find next stage
        List<CustomOrderStage> allStages = stageRepository
                .findByCustomOrder_CustomOrderIdOrderByStageOrderAsc(orderIdForQuery);
        
        // Find next stage
        CustomOrderStage nextStage = allStages.stream()
                .filter(s -> s.getStageOrder().equals(currentStageOrder + 1))
                .findFirst()
                .orElse(null);
        
        // UNLOCK NEXT STAGE for payment
        if (nextStage != null) {
            nextStage.setCanPay(true);  // ← Unlock next stage
            stageRepository.save(nextStage);
            log.info("Unlocked stage {} for payment", nextStage.getStageOrder());
        }
        
        // Notify customer about completion and next stage
        UUID customerId = stage.getCustomOrder().getRequest().getCustomer().getAccountId();
        
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
                    .allMatch(s -> s.getIsCompleted());
            
            if (allCompleted) {
                CustomOrder order = stage.getCustomOrder();
                order.setStatus(CustomOrderStatus.COMPLETED);
                customOrderRepository.save(order);
                log.info("Tất cả giai đoạn đã hoàn thành cho đơn hàng {}", order.getCustomOrderId());
            }
        }
        
        return mapToResponse(stage);
    }
    
    @Override
    public boolean canPayStage(UUID stageId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        // Use workflow flag directly
        return stage.getCanPay() && !stage.getIsPaid();
    }
    
    @Override
    @Transactional
    public CustomOrderStageResponse uploadProofImage(UUID stageId, String imageUrl, UUID artisanId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        // Validate artisan ownership
        if (!stage.getCustomOrder().getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new BadRequestException("Bạn không có quyền upload ảnh cho giai đoạn này");
        }
        
        // Validate stage is paid
        if (stage.getStatus() != StageStatus.PAID && stage.getStatus() != StageStatus.IN_PROGRESS) {
            throw new BadRequestException("Giai đoạn phải được thanh toán trước khi upload ảnh");
        }
        
        stage.setCompletionImageUrl(imageUrl);
        stage = stageRepository.save(stage);
        
        log.info("Artisan {} uploaded proof image for stage {}", artisanId, stageId);
        
        return mapToResponse(stage);
    }
    
    @Override
    @Transactional
    public PaymentInitiationResponse initiateStagePayment(UUID stageId, InitiateStagePaymentRequest paymentRequest, UUID customerId) {
        CustomOrderStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giai đoạn"));
        
        // Validate customer ownership
        if (!stage.getCustomOrder().getRequest().getCustomer().getAccountId().equals(customerId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán giai đoạn này");
        }
        
        // Validate stage is not already paid
        if (stage.getStatus() == StageStatus.PAID) {
            throw new BadRequestException("Giai đoạn này đã được thanh toán");
        }
        
        // Validate this is the next stage to be paid (stages must be paid in order)
        if (!canPayStage(stageId)) {
            throw new BadRequestException("Giai đoạn trước phải được hoàn thành và thanh toán trước");
        }
        
        // Use StagePaymentService to create payment with returnUrl
        String paymentMethodStr = paymentRequest.getPaymentMethod() != null ? 
                paymentRequest.getPaymentMethod().name() : "VNPAY";
        
        // Call the overloaded method that accepts returnUrl
        StagePaymentResponse paymentResponse = ((StagePaymentServiceImp) paymentService).createStagePayment(
                stageId, 
                customerId, 
                paymentMethodStr,
                paymentRequest.getReturnUrl()  // Pass returnUrl from request
        );
        
        // Convert StagePaymentResponse to PaymentInitiationResponse
        PaymentInitiationResponse response = PaymentInitiationResponse.builder()
                .paymentId(paymentResponse.getPaymentId())
                .paymentUrl(paymentResponse.getPaymentUrl())
                .transactionId(paymentResponse.getTransactionId())
                .amount(paymentResponse.getAmount())
                .build();
        
        log.info("Customer {} initiated payment for stage {}", customerId, stageId);
        
        return response;
    }
    
    private CustomOrderStageResponse mapToResponse(CustomOrderStage stage) {
        return CustomOrderStageResponse.builder()
                .stageId(stage.getStageId())
                .orderId(stage.getCustomOrder().getCustomOrderId())
                .stageOrder(stage.getStageOrder())
                .stageName(stage.getName())
                .description(stage.getDescription())
                .amount(stage.getAmount())
                .percentage(stage.getPaymentPercentage())
                .estimatedDays(stage.getEstimatedDays())
                .status(stage.getStatus())
                
                // Workflow flags
                .canPay(stage.getCanPay())
                .isPaid(stage.getIsPaid())
                .isCompleted(stage.getIsCompleted())
                
                // Timestamps
                .dueDate(stage.getDueDate())
                .paidAt(stage.getPaidAt())
                .completedAt(stage.getCompletedAt())
                .createdAt(stage.getCreatedAt())
                
                // Proof
                .completionImageUrl(stage.getCompletionImageUrl())
                
                .build();
    }
}
