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
public class GenerateConceptImageRequest {
    
    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 50, max = 1000, message = "Mô tả phải từ 50 đến 1000 ký tự")
    private String description;
}
