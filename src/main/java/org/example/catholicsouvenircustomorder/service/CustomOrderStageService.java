package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CompleteStageRequest;
import org.example.catholicsouvenircustomorder.dto.response.CustomOrderStageResponse;

import java.util.List;
import java.util.UUID;

public interface CustomOrderStageService {
    CustomOrderStageResponse getStageById(UUID stageId);
    List<CustomOrderStageResponse> getStagesByOrderId(UUID orderId);
    CustomOrderStageResponse completeStage(UUID stageId, CompleteStageRequest request, UUID artisanId);
    boolean canPayStage(UUID stageId);
}
