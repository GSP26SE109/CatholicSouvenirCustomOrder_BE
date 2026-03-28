package org.example.catholicsouvenircustomorder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotationStageDTO {
    
    private Integer stageOrder;
    private String name;
    private String description;
    private BigDecimal amount;
    private Integer paymentPercentage;
}
