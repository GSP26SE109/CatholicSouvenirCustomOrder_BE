package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catholicsouvenircustomorder.model.ShippingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private UUID shipmentId;
    private UUID orderId;
    private UUID customOrderId;
    private String ghnOrderCode;
    private String trackingNumber;
    private ShippingStatus status;
    private String pickAddress;
    private String deliveryAddress;
    private String recipientName;
    private String recipientPhone;
    private BigDecimal shippingFee;
    private BigDecimal insuranceFee;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime actualDelivery;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
