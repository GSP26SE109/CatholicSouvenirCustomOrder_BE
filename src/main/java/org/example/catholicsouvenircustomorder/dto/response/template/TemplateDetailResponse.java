package org.example.catholicsouvenircustomorder.dto.response.template;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TemplateDetailResponse {
    private UUID templateId;
    private UUID artisanId;
    private String artisanName;
    private UUID categoryId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private String material;
    private String style;
    private String basePromptHint;
    private List<String> baseImages;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ZoneResponse> customZones;
}
