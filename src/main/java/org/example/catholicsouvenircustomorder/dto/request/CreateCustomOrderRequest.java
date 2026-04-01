package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateCustomOrderRequest {
    
    @NotNull(message = "Request ID is required")
    private UUID requestId;
    
    @NotNull(message = "Quotation ID is required")
    private UUID quotationId;
    
    @NotEmpty(message = "At least one stage is required")
    private List<StageDefinition> stages;
    
    private String shippingAddress;
    
    @Data
    public static class StageDefinition {
        private String name;
        private String description;
        private Integer paymentPercentage;
    }
}
