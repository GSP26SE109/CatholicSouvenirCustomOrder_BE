package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptureRecommendResponse {
    
    private boolean success;
    private String message;
    private List<ScriptureRecommendation> recommendations;
    private String errorMessage;
}
