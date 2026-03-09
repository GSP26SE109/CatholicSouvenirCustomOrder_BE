package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Data;
import org.example.catholicsouvenircustomorder.model.CustomOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CustomOrderResponse {
    private UUID orderId;
    private UUID requestId;
    private UUID customerId;
    private String customerName;
    private UUID artisanId;
    private String artisanName;
    private BigDecimal totalAmount;
    private CustomOrderStatus status;
    private List<StageInfo> stages;
    private String shippingAddress;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    public static class StageInfo {
        private UUID stageId;
        private String name;
        private String description;
        private Integer stageOrder;
        private Integer paymentPercentage;
        private BigDecimal amount;
        private String status;
        private String completionImageUrl;
        private LocalDateTime completedAt;
        private LocalDateTime paidAt;
        private Boolean canPay; // Helper field for frontend
    }
}
