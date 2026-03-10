package org.example.catholicsouvenircustomorder.dto.response.Dashboard;

import lombok.Builder;
import lombok.Data;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    private DashboardSummary summary;

    private DailyRevenue revenueChart;

    private Map<String, Integer> orderStatus;

    private List<TopProductDTO> topProducts;

    private List<ProductResponse> lowStockProducts;
}
