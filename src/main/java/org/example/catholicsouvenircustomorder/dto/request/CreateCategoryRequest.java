package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {
    
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    private String categoryName;
    
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
    
    private String iconUrl;
    
    @Builder.Default
    private Integer sortOrder = 0;
    
    @Builder.Default
    private Boolean isActive = true;
}
