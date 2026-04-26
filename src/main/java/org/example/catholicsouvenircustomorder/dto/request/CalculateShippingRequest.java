package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateShippingRequest {
    
    @NotNull(message = "District ID không được để trống")
    private Integer toDistrictId;
    
    @NotBlank(message = "Ward code không được để trống")
    private String toWardCode;
    
    // Optional overrides for weight/dimensions
    private Integer weight;  // grams
    private Integer length;  // cm
    private Integer width;   // cm
    private Integer height;  // cm
}
