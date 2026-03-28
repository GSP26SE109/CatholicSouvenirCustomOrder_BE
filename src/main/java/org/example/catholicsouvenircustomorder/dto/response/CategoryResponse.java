package org.example.catholicsouvenircustomorder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for category information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {
    
    private UUID categoryId;
    private String categoryName;
    private String description;
    private Boolean isActive;
    private Integer sortOrder;
    private String iconUrl;
    
    // Statistics
    private Long templateCount;
    private Long productCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
