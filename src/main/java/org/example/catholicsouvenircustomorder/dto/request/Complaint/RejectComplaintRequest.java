package org.example.catholicsouvenircustomorder.dto.request.Complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin to reject complaint
 * Requirements: 3.6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectComplaintRequest {
    
    /**
     * Reason for rejection (20-500 characters)
     * Requirements: 3.6
     */
    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(min = 20, max = 500, message = "Lý do từ chối phải từ 20 đến 500 ký tự")
    private String rejectionReason;
}
