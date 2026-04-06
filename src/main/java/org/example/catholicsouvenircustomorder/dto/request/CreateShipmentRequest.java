package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShipmentRequest {
    
    private UUID orderId;
    private UUID customOrderId;
    
    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String recipientPhone;
    
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String deliveryAddress;
    
    @NotNull(message = "District ID không được để trống")
    private Integer toDistrictId;
    
    @NotBlank(message = "Ward code không được để trống")
    private String toWardCode;
    
    @NotNull(message = "Giá trị đơn hàng không được để trống")
    private BigDecimal orderValue;
    
    private Integer weight = 1000;
    private Integer length = 20;
    private Integer width = 20;
    private Integer height = 10;
    
    private String note;
    
    private Integer serviceTypeId = 2;
    private Integer paymentTypeId = 1;
}
