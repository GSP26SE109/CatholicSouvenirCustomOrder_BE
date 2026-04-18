package org.example.catholicsouvenircustomorder.dto.request.Complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for artisan response to complaint
 * Requirements: 2.3, 2.4, 2.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtisanResponseRequest {
    
    /**
     * Artisan's response/explanation (20-1000 characters)
     * Requirements: 2.3
     */
    @NotBlank(message = "Phản hồi không được để trống")
    @Size(min = 20, max = 1000, message = "Phản hồi phải từ 20 đến 1000 ký tự")
    private String response;
    
    /**
     * Whether artisan requires return of the product
     * Requirements: 2.4, 2.5
     */
    @NotNull(message = "Yêu cầu trả hàng không được để trống")
    private Boolean requireReturn;
}
