package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for customer selecting an artisan for negotiation (Request-Based flow).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectArtisanRequest {
    
    @NotNull(message = "ID nghệ nhân không được để trống")
    private UUID artisanId;
}
