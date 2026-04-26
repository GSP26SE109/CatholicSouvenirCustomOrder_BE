package org.example.catholicsouvenircustomorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeResponse {
    
    private BigDecimal totalShippingFee;
    private Integer totalWeight;
    private List<ArtisanShippingBreakdown> breakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtisanShippingBreakdown {
        private UUID artisanId;
        private String artisanName;
        private BigDecimal shippingFee;
        private Integer weight;
        private Integer itemCount;
        private List<String> productNames;
    }
}
