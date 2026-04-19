package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.response.Dashboard.*;
import org.example.catholicsouvenircustomorder.exception.UnauthorizedException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.OrderStatus;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.AccountService;
import org.example.catholicsouvenircustomorder.service.DashboardService;
import org.example.catholicsouvenircustomorder.service.SystemConfigService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final AccountRepository accountRepository;
    private final ArtisanRepository artisanRepository;
    private final CustomRequestRepository customRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final CustomOrderRepository customOrderRepository;
    private final SystemConfigService systemConfigService;

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
                .customerStats(getCustomerStatistics(start))
                .artisanStats(getArtisanStatistics())
                .customOrderStats(getCustomOrderStatistics(start))
                .complaintStats(getComplaintStatistics(start))
                .revenueBreakdown(getRevenueBreakdown(start))
                .productAnalytics(getProductAnalytics())
                .topCustomers(getTopCustomers())
                .topArtisans(getTopArtisans())
                .build();
    }

    @Override
    public CustomerStatistics getCustomerStatistics(LocalDateTime startDate) {
        return accountRepository.getCustomerStatistics(startDate);
    }

    @Override
    public List<TopCustomerDTO> getTopCustomers() {
        return accountRepository.getTopCustomers(PageRequest.of(0, 10));
    }

    @Override
    public ArtisanStatistics getArtisanStatistics() {
        return artisanRepository.getArtisanStatistics();
    }

    @Override
    public List<TopArtisanDTO> getTopArtisans() {
        return artisanRepository.getTopArtisans(PageRequest.of(0, 10));
    }

    @Override
    public CustomOrderStatistics getCustomOrderStatistics(LocalDateTime startDate) {
        return customRequestRepository.getCustomOrderStatistics(startDate);
    }

    @Override
    public ComplaintStatistics getComplaintStatistics(LocalDateTime startDate) {
        return complaintRepository.getComplaintStatistics(startDate);
    }

    @Override
    public RevenueBreakdown getRevenueBreakdown(LocalDateTime startDate) {
        // Get commission rate from system config (cached in Redis)
        BigDecimal commissionRate = systemConfigService.getCommissionRate();
        
        // Get revenue breakdown from orders
        RevenueBreakdown breakdown = orderRepository.getRevenueBreakdown(startDate, commissionRate);
        
        // Get custom order revenue separately
        BigDecimal customRevenue = customOrderRepository.getCustomOrderRevenue(startDate);
        
        // Combine results using implementation class
        return new RevenueBreakdownImpl(
            breakdown.getProductRevenue(),
            breakdown.getTemplateRevenue(),
            customRevenue,
            breakdown.getTotalCommission()
        );
    }

    /**
     * Inner class implementation of RevenueBreakdown interface
     * Used to combine results from multiple queries
     */
    private static class RevenueBreakdownImpl implements RevenueBreakdown {
        private final BigDecimal productRevenue;
        private final BigDecimal templateRevenue;
        private final BigDecimal customRevenue;
        private final BigDecimal totalCommission;

        public RevenueBreakdownImpl(BigDecimal productRevenue, BigDecimal templateRevenue, 
                                   BigDecimal customRevenue, BigDecimal totalCommission) {
            this.productRevenue = productRevenue;
            this.templateRevenue = templateRevenue;
            this.customRevenue = customRevenue;
            this.totalCommission = totalCommission;
        }

        @Override
        public BigDecimal getProductRevenue() {
            return productRevenue;
        }

        @Override
        public BigDecimal getTemplateRevenue() {
            return templateRevenue;
        }

        @Override
        public BigDecimal getCustomRevenue() {
            return customRevenue;
        }

        @Override
        public BigDecimal getTotalCommission() {
            return totalCommission;
        }
    }

    @Override
    public ProductAnalytics getProductAnalytics() {
        return productRepository.getProductAnalytics();
    }
}
