package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.ConfirmArtisanRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateCustomRequestRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.AIImageService;
import org.example.catholicsouvenircustomorder.service.CustomRequestService;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomRequestServiceImp implements CustomRequestService {

    private final CustomRequestRepository customRequestRepository;
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    private final NotificationService notificationService;
    private final AIImageService aiImageService;

    @Override
    @Transactional
    public CustomRequestResponse createCustomRequest(CreateCustomRequestRequest request, UUID customerId) {
        Account customer = accountRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        List<Artisan> artisans = artisanRepository.findAllById(request.getSelectedArtisanIds());
        if (artisans.size() != request.getSelectedArtisanIds().size()) {
            throw new ResourceNotFoundException("Một số nghệ nhân không tồn tại");
        }

        CustomRequest customRequest = new CustomRequest();
        customRequest.setCustomer(customer);
        customRequest.setTitle(request.getTitle());
        customRequest.setDescription(request.getDescription());
        customRequest.setReferenceImageUrl(request.getReferenceImageUrl());
        customRequest.setSelectedArtisans(artisans);
        customRequest.setStatus(CustomRequestStatus.ARTISAN_SELECTED);

        // Generate AI image if requested
        if (Boolean.TRUE.equals(request.getGenerateAiImage())) {
            String aiImageUrl = aiImageService.generateImage(
                request.getTitle() + ". " + request.getDescription()
            );
            customRequest.setAiGeneratedImageUrl(aiImageUrl);
        }

        customRequest = customRequestRepository.save(customRequest);

        // Send notifications to selected artisans
        for (Artisan artisan : artisans) {
            notificationService.notifyArtisanOfNewCustomRequest(
                artisan.getAccount().getAccountId(),
                customRequest.getRequestId(),
                customer.getFullName(),
                request.getDescription()
            );
        }

        return mapToResponse(customRequest);
    }

    @Override
    public CustomRequestResponse getCustomRequestById(UUID requestId) {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tùy chỉnh"));
        return mapToResponse(request);
    }

    @Override
    public List<CustomRequestResponse> getCustomerRequests(UUID customerId) {
        return customRequestRepository.findByCustomer_AccountId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomRequestResponse> getArtisanRequests(UUID artisanId) {
        return customRequestRepository.findBySelectedArtisans_ArtisanUuid(artisanId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomRequestResponse confirmArtisan(ConfirmArtisanRequest request, UUID customerId) {
        CustomRequest customRequest = customRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tùy chỉnh"));

        if (!customRequest.getCustomer().getAccountId().equals(customerId)) {
            throw new IllegalArgumentException("Bạn không có quyền xác nhận yêu cầu này");
        }

        Artisan artisan = artisanRepository.findById(request.getArtisanId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ nhân"));

        customRequest.setConfirmedArtisan(artisan);
        customRequest.setStatus(CustomRequestStatus.ARTISAN_CONFIRMED);
        customRequest = customRequestRepository.save(customRequest);

        return mapToResponse(customRequest);
    }

    private CustomRequestResponse mapToResponse(CustomRequest request) {
        CustomRequestResponse response = new CustomRequestResponse();
        response.setRequestId(request.getRequestId());
        response.setCustomerId(request.getCustomer().getAccountId());
        response.setCustomerName(request.getCustomer().getFullName());
        response.setTitle(request.getTitle());
        response.setDescription(request.getDescription());
        response.setReferenceImageUrl(request.getReferenceImageUrl());
        response.setAiGeneratedImageUrl(request.getAiGeneratedImageUrl());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt());
        response.setUpdatedAt(request.getUpdatedAt());

        if (request.getSelectedArtisans() != null) {
            response.setSelectedArtisans(request.getSelectedArtisans().stream()
                    .map(this::mapToArtisanBasicInfo)
                    .collect(Collectors.toList()));
        }

        if (request.getConfirmedArtisan() != null) {
            response.setConfirmedArtisan(mapToArtisanBasicInfo(request.getConfirmedArtisan()));
        }

        return response;
    }

    private CustomRequestResponse.ArtisanBasicInfo mapToArtisanBasicInfo(Artisan artisan) {
        CustomRequestResponse.ArtisanBasicInfo info = new CustomRequestResponse.ArtisanBasicInfo();
        info.setArtisanId(artisan.getArtisanUuid());
        info.setArtisanName(artisan.getArtisanName());
        info.setSpecialization(artisan.getSpecialization());
        return info;
    }
}
