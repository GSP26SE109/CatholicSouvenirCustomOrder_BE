package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.*;
import org.example.catholicsouvenircustomorder.exception.UnauthorizedException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.OrderStatus;
import org.example.catholicsouvenircustomorder.repository.OrderDetailRepository;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.AccountService;
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
    private final AccountService accountService;

    // ===== artisan dashboard====//
    @Override
    public DashboardSummary getArtisanDashboardSummary(LocalDateTime start, UUID artisanId) {
        return orderRepository.getSummary(start, artisanId);
    }

    @Override
    public List<DailyRevenue> getArtisanDailyRevenueFromDate(LocalDateTime start, UUID artisanId) {
        return orderRepository.getRevenueFromDate(start, artisanId);
    }

    @Override
    public Map<OrderStatus, Integer> getArtisanOrderStatusStatistic(UUID artisanId) {
        return orderRepository.getOrderStatusRaw(artisanId)
                .stream()
                .collect(Collectors.toMap(
                        row -> OrderStatus.valueOf((String) row[0]),
                        row -> ((Long) row[1]).intValue()
                ));
    }

    @Override
    public List<TopProductDTO> getArtisanMostSoldProducts(UUID artisanId) {
        return orderDetailRepository.getMostSoldProducts(artisanId, PageRequest.of(0, 10));
    }

    @Override
    public List<ShortStockProduct> ArtisanfindShortStockProduct(UUID artisanId) {
        return productRepository.findShortStockProduct(artisanId);
    }

    @Override
    public DashboardResponse getArtisanDashboardInDays(UUID artisanId, int days) {
        Account admin = accountService.findAccountById(artisanId);
        if (!admin.getRole().getName().equals("ARTISAN")) {
            throw new UnauthorizedException("Bạn không có quyền truy cập");
        }
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        return DashboardResponse.builder()
                .summary(getArtisanDashboardSummary(start, artisanId))
                .revenueChart(getArtisanDailyRevenueFromDate(start, artisanId))
                .orderStatus(getArtisanOrderStatusStatistic(artisanId))
                .topProducts(getArtisanMostSoldProducts(artisanId))
                .lowStockProducts(ArtisanfindShortStockProduct(artisanId))
                .build();
    }

    // ===== admin dashboard====//
    @Override
    public DashboardSummary getAdminDashboardSummary(LocalDateTime start) {
        return orderRepository.getAdminSummary(start);
    }

    @Override
    public List<DailyRevenue> getAdminDailyRevenue(LocalDateTime start) {
        return orderRepository.getAdminRevenueFromDate(start);
    }

    @Override
    public Map<OrderStatus, Integer> getAdminOrderStatusStatistic() {
        return orderRepository.getAdminOrderStatusRaw()
                .stream()
                .collect(Collectors.toMap(
                        row -> OrderStatus.valueOf((String) row[0]),
                        row -> ((Long) row[1]).intValue()
                ));
    }

    @Override
    public List<TopProductDTO> getAdminMostSoldProducts() {
        return orderDetailRepository.getAdminMostSoldProducts(PageRequest.of(0, 10));
    }

    @Override
    public DashboardResponse getAdminDashboardInDays(UUID adminId, int days) {
        Account admin = accountService.findAccountById(adminId);
        if (!admin.getRole().getName().equals("ADMIN")) {
            throw new UnauthorizedException("Bạn không có quyền truy cập");
        }
        LocalDateTime start = LocalDateTime.now().minusDays(days);

        return DashboardResponse.builder()
                .summary(getAdminDashboardSummary(start))
                .revenueChart(getAdminDailyRevenue(start))
                .orderStatus(getAdminOrderStatusStatistic())
                .topProducts(getAdminMostSoldProducts())
                .build();
    }
}
