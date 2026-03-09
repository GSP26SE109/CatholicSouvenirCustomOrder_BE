package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CompleteStageRequest {

    @NotNull(message = "Stage ID is required")
    private UUID stageId;

    private String completionImageUrl;
}
