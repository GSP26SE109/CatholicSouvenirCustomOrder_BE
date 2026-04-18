package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptureRecommendation {
    
    private String verse;           // e.g., "John 3:16"
    private String text;            // The actual scripture text
    private String translation;     // Vietnamese/English translation if needed
    private String reason;          // Why this verse is recommended
    private String occasion;        // Suitable occasions
}
