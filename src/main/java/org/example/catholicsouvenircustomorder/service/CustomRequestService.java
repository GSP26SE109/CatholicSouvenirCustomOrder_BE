package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateFreeFormRequestDTO;
import org.example.catholicsouvenircustomorder.dto.request.UpdateDraftRequestDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomRequestService {
    // Request-Based flow methods (CustomRequest only for request-based)
    CustomRequestResponse createFreeFormRequest(CreateFreeFormRequestDTO request, UUID customerId);
    CustomRequestResponse updateDraftRequest(UUID requestId, UpdateDraftRequestDTO request, UUID customerId);
    CustomRequestResponse publishRequest(UUID requestId, UUID customerId);
    CustomRequestResponse regenerateAIImage(UUID requestId, UUID customerId);
    Page<CustomRequestResponse> getOpenRequests(Pageable pageable);
    CustomRequestResponse selectArtisan(UUID requestId, UUID artisanId, UUID customerId);
    void deleteDraftRequest(UUID requestId, UUID customerId);
    
    // Common query methods
    Page<CustomRequestResponse> getCustomerRequests(UUID customerId, CustomRequestStatus status, Pageable pageable);
    Page<CustomRequestResponse> getArtisanRequests(UUID artisanId, CustomRequestStatus status, Pageable pageable);
    CustomRequestResponse getRequestDetail(UUID requestId);
}
