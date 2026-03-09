package org.example.catholicsouvenircustomorder.dto.response;

import lombok.Data;
import org.example.catholicsouvenircustomorder.model.QuotationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class QuotationResponse {
    private UUID quotationId;
    private UUID requestId;
    private UUID artisanId;
    private String artisanName;
    private BigDecimal price;
    private String notes;
    private QuotationStatus status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
