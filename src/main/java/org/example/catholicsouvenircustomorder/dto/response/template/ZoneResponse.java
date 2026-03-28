package org.example.catholicsouvenircustomorder.dto.response.template;

import lombok.Data;
import org.example.catholicsouvenircustomorder.model.InputType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class ZoneResponse {
    private UUID zoneId;
    private String zoneName;
    private String zoneDescription;
    private InputType inputType;
    private Map<String, Object> inputConstraints;
    private BigDecimal extraPrice;
    private Boolean isRequired;
    private Integer sortOrder;
}
