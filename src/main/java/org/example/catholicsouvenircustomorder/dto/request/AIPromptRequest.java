package org.example.catholicsouvenircustomorder.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIPromptRequest {
    
    private String basePromptHint;
    
    private Map<String, String> zoneInputs;
    
    private String additionalDescription;
}
