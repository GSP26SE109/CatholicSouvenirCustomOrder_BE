package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIImageResponse {
    
    private String imageUrl;
    
    private String prompt;
    
    private boolean success;
    
    private String errorMessage;
}
