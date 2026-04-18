package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptureRecommendRequest {
    
    @NotBlank(message = "Mục đích sử dụng không được để trống")
    @Size(max = 500, message = "Mục đích sử dụng không được vượt quá 500 ký tự")
    private String purpose;  // e.g., "Baptism gift", "Wedding anniversary", "Christmas decoration"
    
    @Size(max = 200, message = "Tên sản phẩm không được vượt quá 200 ký tự")
    private String productName;  // e.g., "Cross necklace", "Statue of Mary"
    
    @Size(max = 100, message = "Chủ đề không được vượt quá 100 ký tự")
    private String theme;  // e.g., "Love", "Faith", "Hope", "Protection"
    
    private String language;  // "vi" for Vietnamese, "en" for English, "la" for Latin
    
    private Integer maxResults;  // Number of suggestions to return (default: 3)
}
