package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.CreateOrderWithStagesDTO;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.StageDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.exception.UnauthorizedTemplateAccessException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.repository.CustomOrderRepository;
import org.example.catholicsouvenircustomorder.repository.CustomRequestRepository;
import org.example.catholicsouvenircustomorder.service.CustomOrderService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CustomOrderResponse createFromNegotiation(UUID requestId, UUID artisanId, CreateOrderWithStagesDTO dto) {
        // Use pessimistic lock to prevent race condition
        CustomRequest request = customRequestRepository.findByIdWithLock(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));

        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));

        // Verify artisan is selectedArtisan for the request
        if (request.getSelectedArtisan() == null ||
                !request.getSelectedArtisan().getArtisanUuid().equals(artisanId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không phải là nghệ nhân được chọn cho yêu cầu này");
        }

        // Verify request status = ARTISAN_SELECTED
        if (request.getStatus() != CustomRequestStatus.ARTISAN_SELECTED) {
            throw new BadRequestException("Yêu cầu phải đã chọn nghệ nhân để tạo đơn hàng");
        }

        // Verify requestType = REQUEST_BASED
        if (request.getRequestType() != RequestType.REQUEST_BASED) {
            throw new BadRequestException("Chỉ có thể tạo đơn hàng với giai đoạn từ yêu cầu tự do");
        }

        // Check if order already exists
        if (customOrderRepository.existsByRequestId(requestId)) {
            throw new BadRequestException("Đơn hàng đã tồn tại cho yêu cầu này");
        }

        // Validate stages: percentages sum to 100%, amounts sum to totalPrice
        validateStages(dto.getStages(), dto.getTotalPrice());

        // Create CustomOrder
        CustomOrder customOrder = new CustomOrder();
        customOrder.setRequest(request);
        customOrder.setArtisan(artisan);
        customOrder.setStatus(CustomOrderStatus.PENDING_PAYMENT);
        customOrder.setTotalPrice(dto.getTotalPrice());

        // Create CustomOrderStages from DTO
        List<CustomOrderStage> stages = new ArrayList<>();
        for (int i = 0; i < dto.getStages().size(); i++) {
            StageDTO stageDTO = dto.getStages().get(i);

            CustomOrderStage stage = new CustomOrderStage();
            stage.setCustomOrder(customOrder);
            stage.setStageOrder(i + 1);
            stage.setName(stageDTO.getName());
            stage.setDescription(stageDTO.getDescription());
            stage.setAmount(stageDTO.getAmount());
            stage.setPaymentPercentage(stageDTO.getPaymentPercentage());
            stage.setEstimatedDays(stageDTO.getEstimatedDays());
            stage.setStatus(StageStatus.PENDING);

            stages.add(stage);
        }
        customOrder.setStages(stages);

        customOrder = customOrderRepository.save(customOrder);

        // Update request status = IN_PROGRESS
        request.setStatus(CustomRequestStatus.IN_PROGRESS);
        customRequestRepository.save(request);

        // NOTE: Payment for first stage is NOT created automatically
        // Customer must explicitly initiate payment via /api/stages/{stageId}/payment/initiate
        // This allows customer to choose payment method and timing

        // Notify customer
        notificationService.notifyCustomerOfOrderCreatedWithStages(
                request.getCustomer().getAccountId(),
                customOrder.getCustomOrderId(),
                dto.getTotalPrice().longValue(),
                dto.getStages().size()
        );

        return mapToResponse(customOrder);
    }

    @Override
    public Page<CustomOrderResponse> getCustomerOrders(UUID customerId, Pageable pageable) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        Page<CustomOrder> orders = customOrderRepository.findByRequest_Customer(customer, pageable);
        return orders.map(this::mapToResponse);
    }

    @Override
    public Page<CustomOrderResponse> getCustomerOrders(UUID customerId, CustomOrderStatus status, Pageable pageable) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        Page<CustomOrder> orders = customOrderRepository.findByRequest_CustomerAndStatus(customer, status, pageable);
        return orders.map(this::mapToResponse);
    }

    @Override
    public Page<CustomOrderResponse> getArtisanOrders(UUID artisanId, Pageable pageable) {
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));

        Page<CustomOrder> orders = customOrderRepository.findByArtisan(artisan, pageable);
        return orders.map(this::mapToResponse);
    }

    @Override
    public Page<CustomOrderResponse> getArtisanOrders(UUID artisanId, CustomOrderStatus status, Pageable pageable) {
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));

        Page<CustomOrder> orders = customOrderRepository.findByArtisanAndStatus(artisan, status, pageable);
        return orders.map(this::mapToResponse);
    }

    @Override
    public CustomOrderDetailResponse getOrderDetail(UUID orderId) {
        CustomOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // Check if all stages are paid
        boolean fullyPaid = order.getStages().stream()
                .allMatch(stage -> stage.getStatus() == StageStatus.PAID ||
                        stage.getStatus() == StageStatus.COMPLETED);

        return mapToDetailResponse(order, fullyPaid);
    }

    @Override
    @Transactional
    public CustomOrderResponse updateStatus(UUID orderId, CustomOrderStatus status, UUID userId) {
        CustomOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // Verify artisan ownership for status updates
        if (!order.getArtisan().getAccount().getAccountId().equals(userId)) {
            throw new UnauthorizedTemplateAccessException("Chỉ nghệ nhân mới có thể cập nhật trạng thái đơn hàng");
        }

        // Only allow artisan to update to IN_PRODUCTION or COMPLETED
        if (status != CustomOrderStatus.IN_PRODUCTION && status != CustomOrderStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể cập nhật trạng thái thành IN_PRODUCTION hoặc COMPLETED");
        }

        validateStatusTransition(order.getStatus(), status);

        order.setStatus(status);
        order = customOrderRepository.save(order);

        if (status == CustomOrderStatus.COMPLETED) {
            notificationService.notifyCustomerOfOrderCompletion(
                    order.getRequest().getCustomer().getAccountId(),
                    orderId,
                    order.getArtisan().getAccount().getFullName()
            );
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public CustomOrderResponse cancelOrder(UUID orderId, UUID userId, String reason) {
        CustomOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        validateUserAccess(order, userId);

        if (order.getStatus() == CustomOrderStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy đơn hàng đã hoàn thành");
        }

        if (order.getStatus() == CustomOrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng đã bị hủy");
        }

        // Check if order has been paid and process refund
        for (CustomOrderStage stage : order.getStages()) {
            if (stage.getStatus() == StageStatus.PAID || stage.getStatus() == StageStatus.COMPLETED) {
                // TODO: Implement refund for stage payments via Transaction entity
                log.info("Stage {} needs refund processing", stage.getStageId());
            }
        }

        order.setStatus(CustomOrderStatus.CANCELLED);
        order = customOrderRepository.save(order);

        return mapToResponse(order);
    }

    // ==================== Private Helper Methods ====================

    private void validateStages(List<StageDTO> stages, BigDecimal totalPrice) {
        if (stages == null || stages.isEmpty()) {
            throw new BadRequestException("Phải có ít nhất một giai đoạn thanh toán");
        }

        // Validate percentages sum to 100%
        int totalPercentage = stages.stream()
                .mapToInt(StageDTO::getPaymentPercentage)
                .sum();

        if (totalPercentage != 100) {
            throw new BadRequestException(
                    String.format("Tổng phần trăm thanh toán phải bằng 100%% (hiện tại: %d%%)", totalPercentage)
            );
        }

        // Validate amounts sum to totalPrice
        BigDecimal totalAmount = stages.stream()
                .map(StageDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(totalPrice) != 0) {
            throw new BadRequestException(
                    String.format("Tổng số tiền các giai đoạn (%s) phải bằng tổng giá đơn hàng (%s)",
                            totalAmount, totalPrice)
            );
        }
    }

    private void validateUserAccess(CustomOrder order, UUID userId) {
        boolean isCustomer = order.getRequest().getCustomer().getAccountId().equals(userId);
        boolean isArtisan = order.getArtisan().getAccount().getAccountId().equals(userId);

        if (!isCustomer && !isArtisan) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền truy cập đơn hàng này");
        }
    }

    private void validateStatusTransition(CustomOrderStatus currentStatus, CustomOrderStatus newStatus) {
        if (currentStatus == CustomOrderStatus.CANCELLED) {
            throw new BadRequestException("Không thể thay đổi trạng thái đơn hàng đã hủy");
        }

        if (currentStatus == CustomOrderStatus.COMPLETED && newStatus != CustomOrderStatus.COMPLETED) {
            throw new BadRequestException("Không thể thay đổi trạng thái đơn hàng đã hoàn thành");
        }
    }

    private CustomOrderResponse mapToResponse(CustomOrder order) {
        CustomRequest request = order.getRequest();
        
        // Map stages
        List<CustomOrderStageResponse> stageResponses = order.getStages().stream()
                .map(this::mapToStageResponse)
                .collect(Collectors.toList());

        return CustomOrderResponse.builder()
                .customOrderId(order.getCustomOrderId())
                .requestId(request.getRequestId())
                .customerId(request.getCustomer().getAccountId())
                .customerName(request.getCustomer().getFullName())
                .artisanId(order.getArtisan().getArtisanUuid())
                .artisanName(order.getArtisan().getAccount().getFullName())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .stages(stageResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    private CustomOrderStageResponse mapToStageResponse(CustomOrderStage stage) {
        return CustomOrderStageResponse.builder()
                .stageId(stage.getStageId())
                .orderId(stage.getCustomOrder().getCustomOrderId())
                .stageOrder(stage.getStageOrder())
                .stageName(stage.getName())
                .description(stage.getDescription())
                .amount(stage.getAmount())
                .percentage(stage.getPaymentPercentage())
                .status(stage.getStatus())
                .dueDate(stage.getDueDate())
                .paidAt(stage.getPaidAt())
                .completedAt(stage.getCompletedAt())
                .completionImageUrl(stage.getCompletionImageUrl())
                .createdAt(stage.getCreatedAt())
                .build();
    }

    private CustomOrderDetailResponse mapToDetailResponse(CustomOrder order, boolean fullyPaid) {
        CustomRequest request = order.getRequest();

        return CustomOrderDetailResponse.builder()
                .customOrderId(order.getCustomOrderId())
                .requestId(request.getRequestId())
                .customerId(request.getCustomer().getAccountId())
                .customerName(request.getCustomer().getFullName())
                .customerEmail(request.getCustomer().getEmail())
                .customerPhone(request.getCustomer().getPhone())
                .artisanId(order.getArtisan().getArtisanUuid())
                .artisanName(order.getArtisan().getAccount().getFullName())
                .artisanEmail(order.getArtisan().getAccount().getEmail())
                .artisanPhone(order.getArtisan().getAccount().getPhone())
                .description(request.getDescription())
                .aiConceptImageUrl(request.getAiConceptImageUrl())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .fullyPaid(fullyPaid)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

