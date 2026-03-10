package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface DailyRevenue {
    LocalDateTime getDate();
    BigDecimal getRevenue();
}
