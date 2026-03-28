package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenue {
    LocalDate getDate();
    Long getOrderNumber();
    BigDecimal getRevenue();
}
