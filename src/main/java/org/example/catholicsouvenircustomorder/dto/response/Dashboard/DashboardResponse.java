package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import lombok.Builder;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.OrderStatus;


import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    // Existing fields
    private DashboardSummary summary;

    private List<DailyRevenue> revenueChart;

    private Map<OrderStatus, Integer> orderStatus;

    private List<TopProductDTO> topProducts;

    private List<ShortStockProduct> lowStockProducts;

    // New statistics fields
    private CustomerStatistics customerStats;

    private ArtisanStatistics artisanStats;

    private CustomOrderStatistics customOrderStats;

    private ComplaintStatistics complaintStats;

    private RevenueBreakdown revenueBreakdown;

    private ProductAnalytics productAnalytics;

    private List<TopCustomerDTO> topCustomers;

    private List<TopArtisanDTO> topArtisans;
}
