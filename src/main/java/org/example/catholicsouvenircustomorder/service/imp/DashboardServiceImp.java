package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.Utils.Helper.ProductMapper;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DailyRevenue;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardResponse;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.DashboardSummary;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.TopProductDTO;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.repository.OrderDetailRepository;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImp implements DashboardService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final OrderDetailRepository orderDetailRepository;

    @Override
    public DashboardSummary getDashboardSummary(LocalDateTime start) {
        return orderRepository.getSummary(start);
    }

    @Override
    public DailyRevenue getDailyRevenueFromDate(LocalDateTime startDate) {
        return orderRepository.getRevenueFromDate(startDate);
    }

    @Override
    public Map<String, Integer> getOrderStatusStatistic() {
        return orderRepository.getOrderStatusRaw()
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    @Override
    public List<TopProductDTO> getMostSoldProducts() {
        return orderDetailRepository.getMostSoldProducts();
    }

    @Override
    public List<ProductResponse> findShortStockProduct() {
        return productMapper.toResponseList(productRepository.findShortStockProduct());
    }

    @Override
    public DashboardResponse getDashboardInDays(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        return DashboardResponse.builder()
                .summary(getDashboardSummary(start))
                .revenueChart(getDailyRevenueFromDate(start))
                .orderStatus(getOrderStatusStatistic())
                .topProducts(getMostSoldProducts())
                .lowStockProducts(findShortStockProduct())
                .build();
    }

}
