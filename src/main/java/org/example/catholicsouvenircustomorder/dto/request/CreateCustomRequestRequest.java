package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomRequestRequest {
    
    @NotNull(message = "Template ID không được để trống")
    private UUID templateId;
    
    @NotNull(message = "Zone inputs không được để trống")
    @Builder.Default
    private Map<String, String> zoneInputs = new HashMap<>();
    
    @Size(max = 2000, message = "Mô tả bổ sung không được vượt quá 2000 ký tự")
    private String additionalDescription;
    
    @Builder.Default
    private Boolean generateAiImage = false;
}
