package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.response.Dashboard.*;
import org.example.catholicsouvenircustomorder.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DashboardService {
    DashboardSummary getArtisanDashboardSummary(LocalDateTime start, UUID artisanId);

    List<DailyRevenue> getArtisanDailyRevenueFromDate(LocalDateTime start, UUID artisanId);

    Map<OrderStatus, Integer> getArtisanOrderStatusStatistic(UUID artisanId);

    List<TopProductDTO> getArtisanMostSoldProducts(UUID artisanId);

    List<ShortStockProduct> ArtisanfindShortStockProduct(UUID artisanId);

    DashboardResponse getArtisanDashboardInDays(UUID artisanId, int days);
    DashboardSummary getAdminDashboardSummary(LocalDateTime start);
    List<DailyRevenue> getAdminDailyRevenue(LocalDateTime start);
    Map<OrderStatus, Integer> getAdminOrderStatusStatistic();
    List<TopProductDTO> getAdminMostSoldProducts();
    DashboardResponse getAdminDashboardInDays(UUID adminId, int days);
}
