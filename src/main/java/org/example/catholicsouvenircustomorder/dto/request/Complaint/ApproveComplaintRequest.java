package org.example.catholicsouvenircustomorder.dto.request.Complaint;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for admin to approve complaint
 * Requirements: 3.4, 3.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveComplaintRequest {
    
    /**
     * Refund amount (must be between 1 and order total)
     * Requirements: 3.4
     */
    @NotNull(message = "Số tiền hoàn không được để trống")
    @DecimalMin(value = "1.0", message = "Số tiền hoàn phải lớn hơn 0")
    private BigDecimal refundAmount;
    
    /**
     * Admin's note (10-500 characters)
     * Requirements: 3.5
     */
    @NotBlank(message = "Ghi chú không được để trống")
    @Size(min = 10, max = 500, message = "Ghi chú phải từ 10 đến 500 ký tự")
    private String adminNote;
}
