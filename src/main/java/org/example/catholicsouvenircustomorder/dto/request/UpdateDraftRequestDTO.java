package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a DRAFT custom request.
 * Only title and description can be updated while in DRAFT status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDraftRequestDTO {
    
    @Size(min = 15, max = 1000, message = "Tiêu đề phải từ 15 đến 1000 ký tự")
    private String title;
    
    @Size(min = 50, max = 5000, message = "Mô tả phải từ 50 đến 5000 ký tự")
    private String description;
}
