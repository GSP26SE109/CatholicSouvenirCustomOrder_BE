package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.*;
import org.example.catholicsouvenircustomorder.repository.OrderDetailRepository;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImp implements DashboardService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Override
    public DashboardSummary getDashboardSummary(LocalDateTime start, UUID artisanId) {
        return orderRepository.getSummary(start, artisanId);
    }

    @Override
    public List<DailyRevenue> getDailyRevenueFromDate(LocalDateTime start, UUID artisanId) {
        return orderRepository.getRevenueFromDate(start, artisanId);
    }

    @Override
    public Map<String, Integer> getOrderStatusStatistic(UUID artisanId) {
        return orderRepository.getOrderStatusRaw(artisanId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    @Override
    public List<TopProductDTO> getMostSoldProducts(UUID artisanId) {
        return orderDetailRepository.getMostSoldProducts(artisanId, PageRequest.of(0, 10));
    }

    @Override
    public List<ShortStockProduct> findShortStockProduct(UUID artisanId) {
        return productRepository.findShortStockProduct(artisanId);
    }

    @Override
    public DashboardResponse getDashboardInDays(UUID artisanId, int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        return DashboardResponse.builder()
                .summary(getDashboardSummary(start, artisanId))
                .revenueChart(getDailyRevenueFromDate(start, artisanId))
                .orderStatus(getOrderStatusStatistic(artisanId))
                .topProducts(getMostSoldProducts(artisanId))
                .lowStockProducts(findShortStockProduct(artisanId))
                .build();
    }
}
