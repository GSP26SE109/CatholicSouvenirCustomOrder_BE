package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.AIPromptRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateCustomRequestRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateFreeFormRequestDTO;
import org.example.catholicsouvenircustomorder.dto.response.AIImageResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;
import org.example.catholicsouvenircustomorder.exception.*;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.AIImageService;
import org.example.catholicsouvenircustomorder.service.CustomOrderService;
import org.example.catholicsouvenircustomorder.service.CustomRequestService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.ProductTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomRequestServiceImp implements CustomRequestService {

    private final CustomRequestRepository customRequestRepository;
    private final ProductTemplateRepository templateRepository;
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    private final AIImageService aiImageService;
    private final NotificationService notificationService;
    private final ProductTemplateService productTemplateService;
    private final CustomOrderService customOrderService;

    private static final int MAX_IMAGE_GEN_COUNT = 3;

    @Override
    @Transactional
    public CustomRequestResponse createFromTemplate(CreateCustomRequestRequest request, UUID customerId) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        ProductTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy template"));

        if (!template.getIsActive()) {
            throw new BadRequestException("Template này không còn hoạt động");
        }

        // Validate zone inputs against template constraints
        validateZoneInputs(template, request.getZoneInputs());

        // Create custom request
        CustomRequest customRequest = new CustomRequest();
        customRequest.setCustomer(customer);
        customRequest.setTemplate(template);
        customRequest.setCustomizationData(request.getZoneInputs());
        customRequest.setDescription(request.getAdditionalDescription());
        customRequest.setRequestType(RequestType.TEMPLATE_BASED);
        customRequest.setStatus(CustomRequestStatus.PENDING);
        customRequest.setImageGenCount(0);

        // Generate AI concept image if requested
        if (Boolean.TRUE.equals(request.getGenerateAiImage())) {
            generateAndSetAIImage(customRequest, template, request);
        }

        customRequest = customRequestRepository.save(customRequest);

        // Notify template owner artisan
        notifyArtisanOfNewRequest(template.getArtisan(), customRequest);

        return mapToResponse(customRequest);
    }

    @Override
    @Transactional
    public CustomRequestResponse regenerateAIImage(UUID requestId, UUID customerId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));

        // Verify customer ownership
        if (!customRequest.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền với yêu cầu này");
        }

        // Check image generation count limit
        if (customRequest.getImageGenCount() >= MAX_IMAGE_GEN_COUNT) {
            throw new ImageGenerationLimitExceededException(
                    "Bạn đã hết lượt tạo ảnh (tối đa " + MAX_IMAGE_GEN_COUNT + " lần)"
            );
        }

        // Generate new AI image
        AIPromptRequest aiRequest = new AIPromptRequest();
        aiRequest.setBasePromptHint(customRequest.getTemplate().getBasePromptHint());
        aiRequest.setZoneInputs(customRequest.getCustomizationData());
        aiRequest.setAdditionalDescription(customRequest.getDescription());

        AIImageResponse aiResponse = aiImageService.generateConceptImage(aiRequest);

        // Update AI image data if successful
        if (aiResponse.isSuccess()) {
            customRequest.setAiConceptImageUrl(aiResponse.getImageUrl());
            customRequest.setAiImagePrompt(aiResponse.getPrompt());
        }

        // Increment image generation count
        customRequest.setImageGenCount(customRequest.getImageGenCount() + 1);
        customRequest = customRequestRepository.save(customRequest);

        return mapToResponse(customRequest);
    }

    @Override
    @Transactional
    public CustomRequestResponse acceptRequest(UUID requestId, UUID artisanId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));

        // Verify artisan owns the template
        if (!customRequest.getTemplate().getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền chấp nhận yêu cầu này");
        }

        // Verify request is in PENDING status
        if (customRequest.getStatus() != CustomRequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu không ở trạng thái chờ xử lý");
        }

        // Verify request type is TEMPLATE_BASED
        if (customRequest.getRequestType() != RequestType.TEMPLATE_BASED) {
            throw new BadRequestException("Chỉ có thể chấp nhận yêu cầu dựa trên mẫu");
        }

        // Update request status to ACCEPTED
        customRequest.setStatus(CustomRequestStatus.ACCEPTED);
        customRequest = customRequestRepository.save(customRequest);

        // Create CustomOrder (no stages for Template-Based flow)
        try {
            CustomOrderResponse orderResponse = customOrderService.createFromRequest(requestId, artisanId);

            // Note: Payment record creation is handled by customer when they initiate payment
            // The order is created with status PENDING_PAYMENT, waiting for customer to pay
        } catch (Exception e) {
            // Rollback request status if order creation fails
            customRequest.setStatus(CustomRequestStatus.PENDING);
            customRequestRepository.save(customRequest);
            throw new BadRequestException("Không thể tạo đơn hàng: " + e.getMessage());
        }

        // Notify customer of acceptance
        notifyCustomerOfAcceptance(customRequest);

        return mapToResponse(customRequest);
    }

    @Override
    @Transactional
    public CustomRequestResponse rejectRequest(UUID requestId, UUID artisanId, String reason) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));

        // Verify artisan owns the template
        if (!customRequest.getTemplate().getArtisan().getArtisanUuid().equals(artisanId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền từ chối yêu cầu này");
        }

        // Verify request is in PENDING status
        if (customRequest.getStatus() != CustomRequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu không ở trạng thái chờ xử lý");
        }

        // Update request status to REJECTED
        customRequest.setStatus(CustomRequestStatus.REJECTED);

        // Store rejection reason in description (append to existing description)
        if (reason != null && !reason.trim().isEmpty()) {
            String currentDesc = customRequest.getDescription() != null ? customRequest.getDescription() : "";
            customRequest.setDescription(currentDesc + "\n\n[Lý do từ chối]: " + reason);
        }

        customRequest = customRequestRepository.save(customRequest);

        // Notify customer of rejection
        notifyCustomerOfRejection(customRequest, reason);

        return mapToResponse(customRequest);
    }

    @Override
    public Page<CustomRequestResponse> getCustomerRequests(UUID customerId, CustomRequestStatus status, Pageable pageable) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        Page<CustomRequest> requests;
        if (status != null) {
            requests = customRequestRepository.findByCustomerAndStatus(customer, status, pageable);
        } else {
            requests = customRequestRepository.findByCustomer(customer, pageable);
        }

        return requests.map(this::mapToResponse);
    }

    @Override
    public Page<CustomRequestResponse> getArtisanRequests(UUID artisanId, CustomRequestStatus status, Pageable pageable) {
        artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));

        Page<CustomRequest> requests;
        if (status != null) {
            requests = customRequestRepository.findByTemplate_Artisan_ArtisanUuidAndStatus(artisanId, status, pageable);
        } else {
            requests = customRequestRepository.findByTemplate_Artisan_ArtisanUuid(artisanId, pageable);
        }

        return requests.map(this::mapToResponse);
    }

    @Override
    public CustomRequestResponse getRequestDetail(UUID requestId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));

        return mapToResponse(request);
    }

    // ==================== Request-Based Flow Methods ====================

    @Override
    @Transactional
    public CustomRequestResponse createFreeFormRequest(CreateFreeFormRequestDTO request, UUID customerId) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        // Validate description length (already validated by @Size, but double-check)
        if (request.getDescription() == null || request.getDescription().length() < 50) {
            throw new BadRequestException("Mô tả phải có ít nhất 50 ký tự");
        }

        // Validate budget range
        if (request.getMinBudget() == null || request.getMaxBudget() == null) {
            throw new BadRequestException("Ngân sách tối thiểu và tối đa không được để trống");
        }

        if (request.getMinBudget().compareTo(request.getMaxBudget()) > 0) {
            throw new BadRequestException("Ngân sách tối thiểu không được lớn hơn ngân sách tối đa");
        }

        // Create custom request
        CustomRequest customRequest = new CustomRequest();
        customRequest.setCustomer(customer);
        customRequest.setTemplate(null); // No template for REQUEST_BASED
        customRequest.setCustomizationData(null); // No zone inputs for REQUEST_BASED
        customRequest.setDescription(request.getDescription());
        customRequest.setRequestType(RequestType.REQUEST_BASED);
        customRequest.setStatus(CustomRequestStatus.OPEN);
        customRequest.setMinBudget(request.getMinBudget());
        customRequest.setMaxBudget(request.getMaxBudget());
        customRequest.setImageGenCount(0);

        // Optionally generate AI concept image if customer provides reference
        if (Boolean.TRUE.equals(request.getGenerateAiImage()) &&
                request.getReferenceImages() != null && !request.getReferenceImages().isEmpty()) {
            try {
                AIPromptRequest aiRequest = new AIPromptRequest();
                aiRequest.setAdditionalDescription(request.getDescription());
                // Note: Reference images would need to be handled by AI service

                AIImageResponse aiResponse = aiImageService.generateConceptImage(aiRequest);

                if (aiResponse.isSuccess()) {
                    customRequest.setAiConceptImageUrl(aiResponse.getImageUrl());
                    customRequest.setAiImagePrompt(aiResponse.getPrompt());
                    customRequest.setImageGenCount(1);
                }
            } catch (Exception e) {
                // Continue without AI image
            }
        }

        customRequest = customRequestRepository.save(customRequest);

        return mapToFreeFormResponse(customRequest);
    }

    @Override
    public Page<CustomRequestResponse> getOpenRequests(String category, Pageable pageable) {
        // For now, we don't filter by category as CustomRequest doesn't have category field
        // This can be enhanced later if category filtering is needed
        Page<CustomRequest> requests = customRequestRepository.findOpenRequestsForBidding(pageable);
        return requests.map(this::mapToFreeFormResponse);
    }

    @Override
    @Transactional
    public CustomRequestResponse selectArtisan(UUID requestId, UUID artisanId, UUID customerId) {
        // Verify customer exists
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        // Verify artisan exists
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));

        // Verify request exists
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));

        // Verify request belongs to customer
        if (!customRequest.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền với yêu cầu này");
        }

        // Verify request is OPEN and REQUEST_BASED
        if (customRequest.getRequestType() != RequestType.REQUEST_BASED) {
            throw new BadRequestException("Chỉ có thể chọn nghệ nhân cho yêu cầu tự do");
        }

        if (customRequest.getStatus() != CustomRequestStatus.OPEN) {
            throw new BadRequestException("Yêu cầu không ở trạng thái mở");
        }

        // Update request with selected artisan
        customRequest.setSelectedArtisan(artisan);
        customRequest.setStatus(CustomRequestStatus.NEGOTIATING);
        customRequest = customRequestRepository.save(customRequest);

        // Notify selected artisan
        notifyArtisanOfSelection(artisan, customRequest);

        return mapToFreeFormResponse(customRequest);
    }

    // ==================== Private Helper Methods ====================

    private void validateZoneInputs(ProductTemplate template, Map<String, String> zoneInputs) {
        if (template.getCustomZones() == null || template.getCustomZones().isEmpty()) {
            throw new ZoneValidationException("Template không có vùng tùy chỉnh");
        }

        for (TemplateCustomZone zone : template.getCustomZones()) {
            String zoneIdStr = zone.getZoneId().toString();
            String inputValue = zoneInputs.get(zoneIdStr);

            if (zone.getIsRequired() && (inputValue == null || inputValue.trim().isEmpty())) {
                throw new ZoneValidationException("Vùng '" + zone.getZoneName() + "' là bắt buộc");
            }

            if (inputValue == null || inputValue.trim().isEmpty()) {
                continue;
            }

            validateZoneInput(zone, inputValue);
        }
    }

    private void validateZoneInput(TemplateCustomZone zone, String inputValue) {
        Map<String, Object> constraints = zone.getInputConstraints();

        switch (zone.getInputType()) {
            case TEXT:
                validateTextInput(zone, inputValue, constraints);
                break;
            case COLOR:
                validateColorInput(zone, inputValue, constraints);
                break;
            case SELECT:
                validateSelectInput(zone, inputValue, constraints);
                break;
            case IMAGE:
                break;
        }
    }

    private void validateTextInput(TemplateCustomZone zone, String inputValue, Map<String, Object> constraints) {
        if (constraints != null && constraints.containsKey("maxChars")) {
            int maxChars = ((Number) constraints.get("maxChars")).intValue();
            if (inputValue.length() > maxChars) {
                throw new ZoneValidationException(
                        "Vùng '" + zone.getZoneName() + "' vượt quá " + maxChars + " ký tự"
                );
            }
        }
    }

    private void validateColorInput(TemplateCustomZone zone, String inputValue, Map<String, Object> constraints) {
        if (constraints != null && constraints.containsKey("allowedColors")) {
            @SuppressWarnings("unchecked")
            List<String> allowedColors = (List<String>) constraints.get("allowedColors");
            if (!allowedColors.contains(inputValue)) {
                throw new ZoneValidationException(
                        "Màu '" + inputValue + "' không hợp lệ cho vùng '" + zone.getZoneName() + "'"
                );
            }
        }
    }

    private void validateSelectInput(TemplateCustomZone zone, String inputValue, Map<String, Object> constraints) {
        if (constraints != null && constraints.containsKey("options")) {
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) constraints.get("options");
            if (!options.contains(inputValue)) {
                throw new ZoneValidationException(
                        "Giá trị '" + inputValue + "' không hợp lệ cho vùng '" + zone.getZoneName() + "'"
                );
            }
        }
    }

    private void generateAndSetAIImage(CustomRequest customRequest, ProductTemplate template, CreateCustomRequestRequest request) {
        try {
            AIPromptRequest aiRequest = new AIPromptRequest();
            aiRequest.setBasePromptHint(template.getBasePromptHint());
            aiRequest.setZoneInputs(request.getZoneInputs());
            aiRequest.setAdditionalDescription(request.getAdditionalDescription());

            AIImageResponse aiResponse = aiImageService.generateConceptImage(aiRequest);

            if (aiResponse.isSuccess()) {
                customRequest.setAiConceptImageUrl(aiResponse.getImageUrl());
                customRequest.setAiImagePrompt(aiResponse.getPrompt());
                customRequest.setImageGenCount(1);
            } else {
                customRequest.setImageGenCount(0);
            }
        } catch (Exception e) {
            customRequest.setImageGenCount(0);
        }
    }

    private void notifyArtisanOfNewRequest(Artisan artisan, CustomRequest customRequest) {
        notificationService.notifyArtisanOfNewCustomRequest(
                artisan.getAccount().getAccountId(),
                customRequest.getRequestId(),
                customRequest.getCustomer().getFullName(),
                customRequest.getDescription()
        );
    }

    private void notifyCustomerOfAcceptance(CustomRequest customRequest) {
        notificationService.notifyCustomerOfRequestAcceptance(
                customRequest.getCustomer().getAccountId(),
                customRequest.getRequestId(),
                customRequest.getTemplate().getArtisan().getAccount().getFullName()
        );
    }

    private void notifyCustomerOfRejection(CustomRequest customRequest, String reason) {
        notificationService.notifyCustomerOfRequestRejection(
                customRequest.getCustomer().getAccountId(),
                customRequest.getRequestId(),
                customRequest.getTemplate().getArtisan().getAccount().getFullName(),
                reason
        );
    }

    private void notifyArtisanOfSelection(Artisan artisan, CustomRequest customRequest) {
        notificationService.notifyArtisanOfSelection(
                artisan.getAccount().getAccountId(),
                customRequest.getRequestId(),
                customRequest.getCustomer().getFullName(),
                customRequest.getDescription()
        );
    }

    private CustomRequestResponse mapToResponse(CustomRequest request) {
        // Handle both TEMPLATE_BASED and REQUEST_BASED flows
        if (request.getRequestType() == RequestType.REQUEST_BASED) {
            return mapToFreeFormResponse(request);
        }

        ProductTemplate template = request.getTemplate();
        Artisan artisan = template.getArtisan();

        BigDecimal totalPrice = productTemplateService.calculatePrice(
                template.getTemplateId(),
                request.getCustomizationData()
        );

        return CustomRequestResponse.builder()
                .requestId(request.getRequestId())
                .customerId(request.getCustomer().getAccountId())
                .customerName(request.getCustomer().getFullName())
                .templateId(template.getTemplateId())
                .templateName(template.getName())
                .templateDescription(template.getDescription())
                .basePrice(template.getBasePrice())
                .artisanId(artisan.getArtisanUuid())
                .artisanName(artisan.getAccount().getFullName())
                .zoneInputs(request.getCustomizationData())
                .additionalDescription(request.getDescription())
                .aiConceptImageUrl(request.getAiConceptImageUrl())
                .aiImagePrompt(request.getAiImagePrompt())
                .imageGenCount(request.getImageGenCount())
                .maxImageGenCount(MAX_IMAGE_GEN_COUNT)
                .status(request.getStatus())
                .requestType(request.getRequestType())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .totalPrice(totalPrice)
                .build();
    }

    private CustomRequestResponse mapToFreeFormResponse(CustomRequest request) {
        CustomRequestResponse.CustomRequestResponseBuilder builder = CustomRequestResponse.builder()
                .requestId(request.getRequestId())
                .customerId(request.getCustomer().getAccountId())
                .customerName(request.getCustomer().getFullName())
                .additionalDescription(request.getDescription())
                .aiConceptImageUrl(request.getAiConceptImageUrl())
                .aiImagePrompt(request.getAiImagePrompt())
                .imageGenCount(request.getImageGenCount())
                .maxImageGenCount(MAX_IMAGE_GEN_COUNT)
                .status(request.getStatus())
                .requestType(request.getRequestType())
                .minBudget(request.getMinBudget())
                .maxBudget(request.getMaxBudget())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt());

        // Include selected artisan information if available
        if (request.getSelectedArtisan() != null) {
            builder.artisanId(request.getSelectedArtisan().getArtisanUuid())
                    .artisanName(request.getSelectedArtisan().getAccount().getFullName());
        }

        return builder.build();
    }
}
