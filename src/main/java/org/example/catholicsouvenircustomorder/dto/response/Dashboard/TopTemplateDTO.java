package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface projection for top template data
 * Used to retrieve top performing templates for artisan dashboard
 */
public interface TopTemplateDTO {
    UUID getTemplateId();
    String getTemplateName();
    Long getTotalOrders();
    BigDecimal getTotalRevenue();
}
