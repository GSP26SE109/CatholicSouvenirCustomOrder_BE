package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;

public interface DashboardSummary {
    Long getTotalOrders();
    BigDecimal getTotalRevenue();
}