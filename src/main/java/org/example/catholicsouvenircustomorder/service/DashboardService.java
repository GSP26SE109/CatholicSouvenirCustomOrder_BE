package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DailyRevenue;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardResponse;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardSummary;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.TopProductDTO;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    DashboardSummary getDashboardSummary(LocalDateTime start);
    DailyRevenue getDailyRevenueFromDate(@Param("startDate") LocalDateTime startDate);
    Map<String,Integer> getOrderStatusStatistic();
    List<TopProductDTO> getMostSoldProducts();
    List<ProductResponse> findShortStockProduct();
    DashboardResponse getDashboardInDays(int days);
}
