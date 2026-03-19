package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ConfirmArtisanRequest {
    
    @NotNull(message = "Request ID is required")
    private UUID requestId;
    
    @NotNull(message = "Artisan ID is required")
    private UUID artisanId;
    
    @NotNull(message = "Quotation ID is required")
    private UUID quotationId;
}
