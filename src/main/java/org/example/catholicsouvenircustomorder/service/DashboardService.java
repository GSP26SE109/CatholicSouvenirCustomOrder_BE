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
    
    // New statistics methods for admin dashboard
    CustomerStatistics getCustomerStatistics(LocalDateTime startDate);
    List<TopCustomerDTO> getTopCustomers();
    ArtisanStatistics getArtisanStatistics();
    List<TopArtisanDTO> getTopArtisans();
    CustomOrderStatistics getCustomOrderStatistics(LocalDateTime startDate);
    ComplaintStatistics getComplaintStatistics(LocalDateTime startDate);
    RevenueBreakdown getRevenueBreakdown(LocalDateTime startDate);
    ProductAnalytics getProductAnalytics();
    
    // New statistics methods for artisan dashboard
    ArtisanFinancialDetails getArtisanFinancialDetails(UUID artisanId, LocalDateTime startDate);
    List<WalletBalanceTrend> getWalletBalanceTrend(UUID artisanId, LocalDateTime startDate);
    ArtisanCustomOrderStats getArtisanCustomOrderStats(UUID artisanId, LocalDateTime startDate);
    ArtisanPerformanceMetrics getArtisanPerformanceMetrics(UUID artisanId, LocalDateTime startDate);
    ArtisanCustomerAnalytics getArtisanCustomerAnalytics(UUID artisanId, LocalDateTime startDate);
    List<TopCustomerDTO> getArtisanTopCustomers(UUID artisanId, LocalDateTime startDate);
    ArtisanTemplatePerformance getArtisanTemplatePerformance(UUID artisanId, LocalDateTime startDate);
    List<TopTemplateDTO> getArtisanTopTemplates(UUID artisanId, LocalDateTime startDate);
}
