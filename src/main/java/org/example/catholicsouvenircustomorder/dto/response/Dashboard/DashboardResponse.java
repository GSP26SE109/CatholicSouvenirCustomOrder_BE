package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import lombok.Builder;
import lombok.Data;
import org.example.catholicsouvenircustomorder.model.OrderStatus;


import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    private DashboardSummary summary;

    private List<DailyRevenue> revenueChart;

    private Map<OrderStatus, Integer> orderStatus;

    private List<TopProductDTO> topProducts;

    private List<ShortStockProduct> lowStockProducts;
}
