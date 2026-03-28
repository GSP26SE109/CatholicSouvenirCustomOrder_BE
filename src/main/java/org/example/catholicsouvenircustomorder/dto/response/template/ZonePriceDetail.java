package org.example.catholicsouvenircustomorder.dto.response.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonePriceDetail {
    private UUID zoneId;
    private String zoneName;
    private BigDecimal extraPrice;
}
