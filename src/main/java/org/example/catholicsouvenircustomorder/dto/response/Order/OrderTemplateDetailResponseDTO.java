package org.example.catholicsouvenircustomorder.dto.response.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTemplateDetailResponseDTO {
    private UUID id;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    
    private UUID templateId;
    private String templateName;
    private Map<String, String> customizations;
}
