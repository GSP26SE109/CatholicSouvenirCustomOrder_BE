package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateWithdrawalRequest {
    
    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "50000", message = "Số tiền rút tối thiểu là 50,000 VND")
    @DecimalMax(value = "50000000", message = "Số tiền rút tối đa là 50,000,000 VND")
    private BigDecimal amount;
    
    @NotBlank(message = "Tên ngân hàng không được để trống")
    @Size(max = 100, message = "Tên ngân hàng không được vượt quá 100 ký tự")
    private String bankName;
    
    @NotBlank(message = "Số tài khoản không được để trống")
    @Pattern(regexp = "^[0-9]{9,20}$", message = "Số tài khoản không hợp lệ (phải từ 9-20 chữ số)")
    private String bankAccountNumber;
    
    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(max = 255, message = "Tên chủ tài khoản không được vượt quá 255 ký tự")
    private String bankAccountName;
    
    @NotBlank(message = "Lý do rút tiền không được để trống")
    @Size(min = 10, max = 500, message = "Lý do rút tiền phải có từ 10 đến 500 ký tự")
    private String reason;
}
