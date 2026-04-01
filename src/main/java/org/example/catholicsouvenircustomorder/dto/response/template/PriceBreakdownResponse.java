package org.example.catholicsouvenircustomorder.dto.response.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceBreakdownResponse {
    private BigDecimal basePrice;
    private List<ZonePriceDetail> zonePrices;
    private BigDecimal totalPrice;
}
