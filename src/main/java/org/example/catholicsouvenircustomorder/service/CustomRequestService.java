package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateCustomRequestRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateFreeFormRequestDTO;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;
import org.example.catholicsouvenircustomorder.model.CustomRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomRequestService {
    // Template-Based flow methods
    CustomRequestResponse createFromTemplate(CreateCustomRequestRequest request, UUID customerId);
    CustomRequestResponse regenerateAIImage(UUID requestId, UUID customerId);
    CustomRequestResponse acceptRequest(UUID requestId, UUID artisanId);
    CustomRequestResponse rejectRequest(UUID requestId, UUID artisanId, String reason);
    
    // Request-Based flow methods
    CustomRequestResponse createFreeFormRequest(CreateFreeFormRequestDTO request, UUID customerId);
    Page<CustomRequestResponse> getOpenRequests(String category, Pageable pageable);
    CustomRequestResponse selectArtisan(UUID requestId, UUID artisanId, UUID customerId);
    
    // Common query methods
    Page<CustomRequestResponse> getCustomerRequests(UUID customerId, CustomRequestStatus status, Pageable pageable);
    Page<CustomRequestResponse> getArtisanRequests(UUID artisanId, CustomRequestStatus status, Pageable pageable);
    CustomRequestResponse getRequestDetail(UUID requestId);
}
