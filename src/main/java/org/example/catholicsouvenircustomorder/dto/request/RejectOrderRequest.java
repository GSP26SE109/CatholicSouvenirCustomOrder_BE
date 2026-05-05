package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for rejecting a custom order before payment
 * Only allowed when order is in PENDING_CONFIRMATION status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectOrderRequest {
    
    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(min = 10, max = 500, message = "Lý do từ chối phải từ 10 đến 500 ký tự")
    private String reason;
}
