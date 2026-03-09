package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateCustomOrderRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderResponse;

import java.util.List;
import java.util.UUID;

public interface CustomOrderService {
    CustomOrderResponse createCustomOrder(CreateCustomOrderRequest request, UUID artisanId);
    CustomOrderResponse getCustomOrderById(UUID orderId);
    List<CustomOrderResponse> getCustomerOrders(UUID customerId);
    List<CustomOrderResponse> getArtisanOrders(UUID artisanId);
    CustomOrderResponse completeStage(CompleteStageRequest request, UUID artisanId);
    CustomOrderResponse updateOrderStatus(UUID orderId, String status);
}
