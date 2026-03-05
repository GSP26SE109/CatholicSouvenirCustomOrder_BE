package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApproveProductRequest {
    @NotBlank(message = "Status không được để trống")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "Status phải là APPROVED hoặc REJECTED")
    private String status;
    
    private String rejectionReason; // Bắt buộc khi status = REJECTED
}
