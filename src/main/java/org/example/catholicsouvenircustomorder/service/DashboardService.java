package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.*;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DashboardService {
    DashboardSummary getDashboardSummary(LocalDateTime start, UUID artisanId);

    List<DailyRevenue> getDailyRevenueFromDate(LocalDateTime start, UUID artisanId);

    Map<String, Integer> getOrderStatusStatistic(UUID artisanId);

    List<TopProductDTO> getMostSoldProducts(UUID artisanId);

    List<ShortStockProduct> findShortStockProduct(UUID artisanId);

    DashboardResponse getDashboardInDays(UUID artisanId, int days);
}
