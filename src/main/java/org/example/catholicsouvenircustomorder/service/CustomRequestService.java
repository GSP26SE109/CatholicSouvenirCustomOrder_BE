package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.ConfirmArtisanRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateCustomRequestRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomRequestResponse;

import java.util.List;
import java.util.UUID;

public interface CustomRequestService {
    CustomRequestResponse createCustomRequest(CreateCustomRequestRequest request, UUID customerId);
    CustomRequestResponse getCustomRequestById(UUID requestId);
    List<CustomRequestResponse> getCustomerRequests(UUID customerId);
    List<CustomRequestResponse> getArtisanRequests(UUID artisanId);
    CustomRequestResponse confirmArtisan(ConfirmArtisanRequest request, UUID customerId);
}
