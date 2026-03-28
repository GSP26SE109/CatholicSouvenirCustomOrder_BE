package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.CreateOrderWithStagesDTO;
import org.example.catholicsouvenircustomorder.dto.request.InitiatePaymentDTO;
import org.example.catholicsouvenircustomorder.dto.request.StageDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderDetailResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;
import org.example.catholicsouvenircustomorder.dto.response.PaymentResponse;
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
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.example.catholicsouvenircustomorder.service.ProductTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOrderServiceImp implements CustomOrderService {

    private final CustomOrderRepository customOrderRepository;
    private final CustomRequestRepository customRequestRepository;
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    private final ProductTemplateService productTemplateService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CustomOrderResponse createFromRequest(UUID requestId, UUID artisanId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));
        
        // Verify request is ACCEPTED
        if (request.getStatus() != CustomRequestStatus.ACCEPTED) {
            throw new BadRequestException("Yêu cầu phải được chấp nhận trước khi tạo đơn hàng");
        }
        
        // Verify requestType = TEMPLATE_BASED
        if (request.getRequestType() != RequestType.TEMPLATE_BASED) {
            throw new BadRequestException("Chỉ có thể tạo đơn hàng từ yêu cầu dựa trên mẫu");
        }
        
        // Verify artisan owns the template
        if (!request.getTemplate().getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền tạo đơn hàng cho yêu cầu này");
        }
        
        // Check if order already exists
        if (customOrderRepository.existsByRequestId(requestId)) {
            throw new BadRequestException("Đơn hàng đã tồn tại cho yêu cầu này");
        }
        
        // Calculate totalPrice from template
        BigDecimal totalPrice = productTemplateService.calculatePrice(
            request.getTemplate().getTemplateId(),
            request.getCustomizationData()
        );
        
        // Create CustomOrder with empty stages list (Template-Based has no stages)
        CustomOrder customOrder = new CustomOrder();
        customOrder.setRequest(request);
        customOrder.setArtisan(artisan);
        customOrder.setStatus(CustomOrderStatus.PENDING_PAYMENT);
        customOrder.setTotalPrice(totalPrice);
        // stages list is empty by default for Template-Based orders
        
        customOrder = customOrderRepository.save(customOrder);
        
        // Update request status to IN_PROGRESS
        request.setStatus(CustomRequestStatus.IN_PROGRESS);
        customRequestRepository.save(request);
        
        return mapToResponse(customOrder);
    }

    @Override
    @Transactional
    public CustomOrderResponse createFromNegotiation(UUID requestId, UUID artisanId, CreateOrderWithStagesDTO dto) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));
        
        // Verify artisan is selectedArtisan for the request
        if (request.getSelectedArtisan() == null || 
            !request.getSelectedArtisan().getArtisanUuid().equals(artisanId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không phải là nghệ nhân được chọn cho yêu cầu này");
        }
        
        // Verify request status = NEGOTIATING
        if (request.getStatus() != CustomRequestStatus.NEGOTIATING) {
            throw new BadRequestException("Yêu cầu phải ở trạng thái đàm phán để tạo đơn hàng");
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
            stage.setStatus(StageStatus.PENDING);
            
            stages.add(stage);
        }
        customOrder.setStages(stages);
        
        customOrder = customOrderRepository.save(customOrder);
        
        // Update request status = IN_PROGRESS
        request.setStatus(CustomRequestStatus.IN_PROGRESS);
        customRequestRepository.save(request);
        
        // Create Payment for first stage
        if (!stages.isEmpty()) {
            CustomOrderStage firstStage = stages.get(0);
            InitiatePaymentDTO paymentDTO = InitiatePaymentDTO.builder()
                    .customOrderId(customOrder.getCustomOrderId())
                    .stageId(firstStage.getStageId())
                    .method(PaymentMethod.VNPAY) // Default method, customer can change later
                    .build();
            
            try {
                paymentService.initiatePayment(paymentDTO);
            } catch (Exception e) {
                // Log error but don't fail order creation
                // Customer can initiate payment later
            }
        }
        
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
        
        // Get payment information
        List<PaymentResponse> payments = paymentService.getCustomOrderPayments(orderId);
        boolean fullyPaid = paymentService.isCustomOrderFullyPaid(orderId);
        
        return mapToDetailResponse(order, payments, fullyPaid);
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
        List<PaymentResponse> payments = paymentService.getCustomOrderPayments(orderId);
        for (PaymentResponse payment : payments) {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                // Refund the payment
                paymentService.refundPayment(payment.getPaymentId(), reason);
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
        ProductTemplate template = request.getTemplate();
        
        // Handle null template for Request-Based flow
        UUID templateId = template != null ? template.getTemplateId() : null;
        String templateName = template != null ? template.getName() : null;
        
        return CustomOrderResponse.builder()
                .customOrderId(order.getCustomOrderId())
                .requestId(request.getRequestId())
                .customerId(request.getCustomer().getAccountId())
                .customerName(request.getCustomer().getFullName())
                .artisanId(order.getArtisan().getArtisanUuid())
                .artisanName(order.getArtisan().getAccount().getFullName())
                .templateId(templateId)
                .templateName(templateName)
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private CustomOrderDetailResponse mapToDetailResponse(CustomOrder order, List<PaymentResponse> payments, boolean fullyPaid) {
        CustomRequest request = order.getRequest();
        ProductTemplate template = request.getTemplate();
        
        // Handle null template for Request-Based flow
        UUID templateId = template != null ? template.getTemplateId() : null;
        String templateName = template != null ? template.getName() : null;
        String templateDescription = template != null ? template.getDescription() : null;
        BigDecimal basePrice = template != null ? template.getBasePrice() : null;
        
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
                .templateId(templateId)
                .templateName(templateName)
                .templateDescription(templateDescription)
                .basePrice(basePrice)
                .zoneInputs(request.getCustomizationData())
                .additionalDescription(request.getDescription())
                .aiConceptImageUrl(request.getAiConceptImageUrl())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .payments(payments)
                .fullyPaid(fullyPaid)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
