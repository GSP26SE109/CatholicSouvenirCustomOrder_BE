package org.example.catholicsouvenircustomorder.dto.request.Complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a complaint
 * Requirements: 1.2, 1.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintRequest {
    
    /**
     * Order ID (nullable if customOrderId is provided)
     */
    private UUID orderId;
    
    /**
     * Custom Order ID (nullable if orderId is provided)
     */
    private UUID customOrderId;
    
    /**
     * Product ID (optional, for orders with multiple products)
     */
    private UUID productId;
    
    /**
     * Reason for complaint (20-1000 characters)
     * Requirements: 1.2
     */
    @NotBlank(message = "Lý do khiếu nại không được để trống")
    @Size(min = 20, max = 1000, message = "Lý do khiếu nại phải từ 20 đến 1000 ký tự")
    private String reason;
    
    /**
     * Evidence images (max 5 images)
     * Requirements: 1.3
     */
    @Size(max = 5, message = "Tối đa 5 hình ảnh bằng chứng")
    private List<String> evidenceImages = new ArrayList<>();
}
