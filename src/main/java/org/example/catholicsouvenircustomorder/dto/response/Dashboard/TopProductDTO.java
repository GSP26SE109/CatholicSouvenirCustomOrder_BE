package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

public record TopProductDTO(
        Integer productId,
        String name,
        Long sold,
        BigDecimal revenue
) {}
