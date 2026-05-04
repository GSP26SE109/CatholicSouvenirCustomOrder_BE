package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.AIPromptRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateFreeFormRequestDTO;
import org.example.catholicsouvenircustomorder.dto.request.UpdateDraftRequestDTO;
import org.example.catholicsouvenircustomorder.dto.response.AIImageResponse;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;
import org.example.catholicsouvenircustomorder.exception.*;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.AIImageService;
import org.example.catholicsouvenircustomorder.service.CustomRequestService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomRequestServiceImp implements CustomRequestService {

    private final CustomRequestRepository customRequestRepository;
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    private final AIImageService aiImageService;
    private final NotificationService notificationService;
    
    private static final int MAX_IMAGE_GEN_COUNT = 3;

    @Override
    @Transactional
    public CustomRequestResponse createFreeFormRequest(CreateFreeFormRequestDTO request, UUID customerId) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
        
        // Validate description length
        if (request.getDescription() == null || request.getDescription().length() < 50) {
            throw new BadRequestException("Mô tả phải có ít nhất 50 ký tự");
        }
        
        // AI concept image is optional - if not provided, can be generated later
        // This allows testing when AI service is unavailable
        
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
        customRequest.setTitle(request.getTitle());
        customRequest.setDescription(request.getDescription());
        customRequest.setRequestType(RequestType.REQUEST_BASED);
        customRequest.setStatus(CustomRequestStatus.DRAFT); // Start as DRAFT
        customRequest.setMinBudget(request.getMinBudget());
        customRequest.setMaxBudget(request.getMaxBudget());
        customRequest.setAiConceptImageUrl(request.getAiConceptImageUrl());
        customRequest.setAiImagePrompt(request.getAiImagePrompt());
        customRequest.setImageGenCount(0);
        
        customRequest = customRequestRepository.save(customRequest);
        
        return mapToResponse(customRequest);
    }

    @Override
    @Transactional
    public CustomRequestResponse updateDraftRequest(UUID requestId, UpdateDraftRequestDTO request, UUID customerId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        
        // Verify customer ownership
        if (!customRequest.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền với yêu cầu này");
        }
        
        // Verify request is in DRAFT status
        if (customRequest.getStatus() != CustomRequestStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể cập nhật yêu cầu ở trạng thái nháp");
        }
        
        // Update title if provided
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            customRequest.setTitle(request.getTitle());
        }
        
        // Update description if provided
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            customRequest.setDescription(request.getDescription());
        }
        
        // Do NOT reset imageGenCount when updating
        customRequest = customRequestRepository.save(customRequest);
        
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
    public CustomRequestResponse publishRequest(UUID requestId, UUID customerId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        
        // Verify customer ownership
        if (!customRequest.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền với yêu cầu này");
        }
        
        // Verify request is in DRAFT status
        if (customRequest.getStatus() != CustomRequestStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể publish yêu cầu ở trạng thái nháp");
        }
        
        // Validate AI concept image is present
        if (customRequest.getAiConceptImageUrl() == null || customRequest.getAiConceptImageUrl().trim().isEmpty()) {
            throw new BadRequestException("Yêu cầu phải có ảnh concept");
        }
        
        // Update status to OPEN
        customRequest.setStatus(CustomRequestStatus.OPEN);
        customRequest = customRequestRepository.save(customRequest);
        
        // Get all artisans and send notification to each
        List<Artisan> allArtisans = artisanRepository.findAll();
        
        log.info("Broadcasting new request {} to {} artisans", 
                customRequest.getRequestId(), allArtisans.size());
        
        for (Artisan artisan : allArtisans) {
            try {
                notificationService.notifyArtisanOfNewCustomRequest(
                    artisan.getAccount().getAccountId(),
                    customRequest.getRequestId(),
                    customRequest.getCustomer().getFullName(),
                    customRequest.getDescription(),
                    customRequest.getAiConceptImageUrl()
                );
            } catch (Exception e) {
                log.error("Failed to notify artisan {}: {}", 
                        artisan.getArtisanUuid(), e.getMessage());
            }
        }
        
        return mapToResponse(customRequest);
    }

    @Override
    public Page<CustomRequestResponse> getOpenRequests(Pageable pageable) {
        // Get all OPEN requests for artisans to browse
        Page<CustomRequest> requests = customRequestRepository.findOpenRequestsForBidding(pageable);
        return requests.map(this::mapToResponse);
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
        
        // Check if artisan is blacklisted
        if (artisan.isBlacklisted()) {
            throw new BadRequestException("Nghệ nhân này đã bị đưa vào danh sách đen và không thể nhận đơn hàng mới");
        }
        
        // Verify request exists
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        
        // Verify request belongs to customer
        if (!customRequest.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền với yêu cầu này");
        }
        
        // Verify request is OPEN
        if (customRequest.getStatus() != CustomRequestStatus.OPEN) {
            throw new BadRequestException("Yêu cầu không ở trạng thái mở");
        }
        
        // Update request with selected artisan
        customRequest.setSelectedArtisan(artisan);
        customRequest.setStatus(CustomRequestStatus.ARTISAN_SELECTED);
        customRequest = customRequestRepository.save(customRequest);
        
        // Notify selected artisan
        notifyArtisanOfSelection(artisan, customRequest);
        
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
            requests = customRequestRepository.findAllByArtisanAndStatus(artisanId, status, pageable);
        } else {
            requests = customRequestRepository.findAllByArtisan(artisanId, pageable);
        }
        
        return requests.map(this::mapToResponse);
    }

    @Override
    public CustomRequestResponse getRequestDetail(UUID requestId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        
        return mapToResponse(request);
    }

    @Override
    @Transactional
    public void deleteDraftRequest(UUID requestId, UUID customerId) {
        CustomRequest customRequest = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        
        // Verify customer ownership
        if (!customRequest.getCustomer().getAccountId().equals(customerId)) {
            throw new UnauthorizedTemplateAccessException("Bạn không có quyền xóa yêu cầu này");
        }
        
        // Only allow deletion of DRAFT requests
        if (customRequest.getStatus() != CustomRequestStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể xóa yêu cầu ở trạng thái nháp. Yêu cầu hiện tại đang ở trạng thái: " + customRequest.getStatus());
        }
        
        log.info("Deleting draft custom request: {}", requestId);
        customRequestRepository.delete(customRequest);
    }

    // ==================== Private Helper Methods ====================

    private void notifyArtisanOfSelection(Artisan artisan, CustomRequest customRequest) {
        notificationService.notifyArtisanOfSelection(
            artisan.getAccount().getAccountId(),
            customRequest.getRequestId(),
            customRequest.getCustomer().getFullName(),
            customRequest.getDescription()
        );
    }

    private CustomRequestResponse mapToResponse(CustomRequest request) {
        CustomRequestResponse.CustomRequestResponseBuilder builder = CustomRequestResponse.builder()
                .requestId(request.getRequestId())
                .customerId(request.getCustomer().getAccountId())
                .customerName(request.getCustomer().getFullName())
                .title(request.getTitle())  // Add title
                .description(request.getDescription())
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
