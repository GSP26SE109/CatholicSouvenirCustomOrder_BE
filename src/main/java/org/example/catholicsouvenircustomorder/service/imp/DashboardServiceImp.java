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
    
    // New dependencies for artisan dashboard statistics
    private final WalletTransactionRepository walletTransactionRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final ConversationRepository conversationRepository;
    private final ProductTemplateRepository productTemplateRepository;
    private final FeedbackRepository feedbackRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final WalletRepository walletRepository;

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
                .financialDetails(getArtisanFinancialDetails(artisanId, start))
                .artisanCustomOrderStats(getArtisanCustomOrderStats(artisanId, start))
                .performanceMetrics(getArtisanPerformanceMetrics(artisanId, start))
                .customerAnalytics(getArtisanCustomerAnalytics(artisanId, start))
                .templatePerformance(getArtisanTemplatePerformance(artisanId, start))
                .artisanTopCustomers(getArtisanTopCustomers(artisanId, start))
                .topTemplates(getArtisanTopTemplates(artisanId, start))
                .refundImpact(getArtisanRefundImpact(artisanId, start))
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
                .refundStats(getRefundStatistics(start))
                .withdrawalStats(getWithdrawalStatistics(start))
                .platformFinancials(getPlatformFinancials(start))
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
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    @Override
    public ArtisanFinancialDetails getArtisanFinancialDetails(UUID artisanId, LocalDateTime startDate) {
        // Get financial summary from wallet transactions
        WalletTransactionRepository.ArtisanFinancialSummary summary = 
            walletTransactionRepository.getFinancialSummary(artisanId, startDate);
        
        // Get pending withdrawal amount
        BigDecimal pendingWithdrawal = withdrawalRequestRepository.getPendingWithdrawalAmount(artisanId);
        
        // Get current balance
        BigDecimal currentBalance = walletTransactionRepository.getCurrentBalance(artisanId);
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        
        // Create implementation class to combine results
        return new ArtisanFinancialDetailsImpl(
            summary.getGrossEarnings(),
            summary.getTotalCommission(),
            pendingWithdrawal,
            currentBalance
        );
    }
    
    @Override
    public List<WalletBalanceTrend> getWalletBalanceTrend(UUID artisanId, LocalDateTime startDate) {
        return walletTransactionRepository.getWalletBalanceTrend(artisanId, startDate);
    }
    
    @Override
    public ArtisanCustomOrderStats getArtisanCustomOrderStats(UUID artisanId, LocalDateTime startDate) {
        // Get partial stats from custom request repository
        CustomRequestRepository.ArtisanCustomOrderStatsPartial partial = 
            customRequestRepository.getCustomOrderStats(artisanId, startDate);
        
        // Get pending requests count
        Long pendingRequests = customRequestRepository.getPendingRequestsCount(artisanId);
        
        // Get average completion time
        Double avgCompletionDays = customOrderRepository.getAvgCompletionTimeDays(artisanId, startDate);
        
        // Create implementation class to combine results
        return new ArtisanCustomOrderStatsImpl(
            partial.getTotalRequests(),
            partial.getTotalOrders(),
            partial.getAvgOrderValue(),
            partial.getConversionRate(),
            pendingRequests,
            avgCompletionDays
        );
    }
    
    @Override
    public ArtisanPerformanceMetrics getArtisanPerformanceMetrics(UUID artisanId, LocalDateTime startDate) {
        // Get rating stats
        FeedbackRepository.ArtisanRatingStats ratingStats = 
            feedbackRepository.getRatingStats(artisanId);
        
        // Get average response time
        Double avgResponseTimeHours = conversationRepository.getAvgResponseTimeHours(artisanId);
        
        // Get order fulfillment rate
        Double orderFulfillmentRate = orderRepository.getOrderFulfillmentRate(artisanId);
        
        // Get on-time delivery rate
        Double onTimeDeliveryRate = orderRepository.getOnTimeDeliveryRate(artisanId);
        
        // Calculate total orders for complaint rate
        Long totalOrders = orderRepository.count();
        
        // Get complaint rate
        Double complaintRate = complaintRepository.getComplaintRate(artisanId, totalOrders);
        
        // Create implementation class to combine results
        return new ArtisanPerformanceMetricsImpl(
            ratingStats.getAvgRating(),
            ratingStats.getTotalReviews(),
            avgResponseTimeHours,
            orderFulfillmentRate,
            complaintRate,
            onTimeDeliveryRate
        );
    }
    
    @Override
    public ArtisanCustomerAnalytics getArtisanCustomerAnalytics(UUID artisanId, LocalDateTime startDate) {
        // Get customer stats
        AccountRepository.ArtisanCustomerStats customerStats = 
            accountRepository.getCustomerStats(artisanId, startDate);
        
        // Get rating stats for satisfaction score
        FeedbackRepository.ArtisanRatingStats ratingStats = 
            feedbackRepository.getRatingStats(artisanId);
        
        // Calculate customer satisfaction score (avgRating / 5.0 * 100)
        Double customerSatisfactionScore = null;
        if (ratingStats.getAvgRating() != null) {
            customerSatisfactionScore = (ratingStats.getAvgRating() / 5.0) * 100;
        }
        
        // Create implementation class to combine results
        return new ArtisanCustomerAnalyticsImpl(
            customerStats.getTotalCustomers(),
            customerStats.getRepeatRate(),
            customerSatisfactionScore
        );
    }
    
    @Override
    public List<TopCustomerDTO> getArtisanTopCustomers(UUID artisanId, LocalDateTime startDate) {
        return accountRepository.getArtisanTopCustomers(artisanId, startDate, PageRequest.of(0, 10));
    }
    
    @Override
    public ArtisanTemplatePerformance getArtisanTemplatePerformance(UUID artisanId, LocalDateTime startDate) {
        // Get total templates count
        Long totalTemplates = productTemplateRepository.getTotalTemplatesCount(artisanId);
        
        // Get template conversion rate
        Double templateConversionRate = productTemplateRepository.getTemplateConversionRate(artisanId);
        
        // Create implementation class to combine results
        return new ArtisanTemplatePerformanceImpl(
            totalTemplates,
            templateConversionRate
        );
    }
    
    @Override
    public List<TopTemplateDTO> getArtisanTopTemplates(UUID artisanId, LocalDateTime startDate) {
        return productTemplateRepository.getTopTemplates(artisanId, PageRequest.of(0, 10));
    }
    
    // ==================== Implementation Classes ====================
    
    /**
     * Inner class implementation of ArtisanFinancialDetails interface
     * Used to combine results from multiple queries
     */
    private static class ArtisanFinancialDetailsImpl implements ArtisanFinancialDetails {
        private final BigDecimal grossEarnings;
        private final BigDecimal totalCommission;
        private final BigDecimal pendingWithdrawal;
        private final BigDecimal currentBalance;
        
        public ArtisanFinancialDetailsImpl(BigDecimal grossEarnings, BigDecimal totalCommission,
                                          BigDecimal pendingWithdrawal, BigDecimal currentBalance) {
            this.grossEarnings = grossEarnings != null ? grossEarnings : BigDecimal.ZERO;
            this.totalCommission = totalCommission != null ? totalCommission : BigDecimal.ZERO;
            this.pendingWithdrawal = pendingWithdrawal != null ? pendingWithdrawal : BigDecimal.ZERO;
            this.currentBalance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        }
        
        @Override
        public BigDecimal getGrossEarnings() {
            return grossEarnings;
        }
        
        @Override
        public BigDecimal getTotalCommission() {
            return totalCommission;
        }
        
        @Override
        public BigDecimal getNetEarnings() {
            return grossEarnings.subtract(totalCommission);
        }
        
        @Override
        public BigDecimal getPendingWithdrawal() {
            return pendingWithdrawal;
        }
        
        @Override
        public BigDecimal getCurrentBalance() {
            return currentBalance;
        }
    }
    
    /**
     * Inner class implementation of ArtisanCustomOrderStats interface
     * Used to combine results from multiple queries
     */
    private static class ArtisanCustomOrderStatsImpl implements ArtisanCustomOrderStats {
        private final Long totalRequests;
        private final Long totalOrders;
        private final BigDecimal avgOrderValue;
        private final Double conversionRate;
        private final Long pendingRequests;
        private final Double avgCompletionDays;
        
        public ArtisanCustomOrderStatsImpl(Long totalRequests, Long totalOrders,
                                          BigDecimal avgOrderValue, Double conversionRate,
                                          Long pendingRequests, Double avgCompletionDays) {
            this.totalRequests = totalRequests != null ? totalRequests : 0L;
            this.totalOrders = totalOrders != null ? totalOrders : 0L;
            this.avgOrderValue = avgOrderValue != null ? avgOrderValue : BigDecimal.ZERO;
            this.conversionRate = conversionRate;
            this.pendingRequests = pendingRequests != null ? pendingRequests : 0L;
            this.avgCompletionDays = avgCompletionDays;
        }
        
        @Override
        public Long getTotalRequests() {
            return totalRequests;
        }
        
        @Override
        public Long getTotalOrders() {
            return totalOrders;
        }
        
        @Override
        public BigDecimal getAvgOrderValue() {
            return avgOrderValue;
        }
        
        @Override
        public Double getConversionRate() {
            return conversionRate;
        }
        
        @Override
        public Long getPendingRequests() {
            return pendingRequests;
        }
        
        @Override
        public Double getAvgCompletionDays() {
            return avgCompletionDays;
        }
    }
    
    /**
     * Inner class implementation of ArtisanPerformanceMetrics interface
     * Used to combine results from multiple queries
     */
    private static class ArtisanPerformanceMetricsImpl implements ArtisanPerformanceMetrics {
        private final Double avgRating;
        private final Long totalReviews;
        private final Double avgResponseTimeHours;
        private final Double orderFulfillmentRate;
        private final Double complaintRate;
        private final Double onTimeDeliveryRate;
        
        public ArtisanPerformanceMetricsImpl(Double avgRating, Long totalReviews,
                                            Double avgResponseTimeHours, Double orderFulfillmentRate,
                                            Double complaintRate, Double onTimeDeliveryRate) {
            this.avgRating = avgRating;
            this.totalReviews = totalReviews != null ? totalReviews : 0L;
            this.avgResponseTimeHours = avgResponseTimeHours;
            this.orderFulfillmentRate = orderFulfillmentRate;
            this.complaintRate = complaintRate;
            this.onTimeDeliveryRate = onTimeDeliveryRate;
        }
        
        @Override
        public Double getAvgRating() {
            return avgRating;
        }
        
        @Override
        public Long getTotalReviews() {
            return totalReviews;
        }
        
        @Override
        public Double getAvgResponseTimeHours() {
            return avgResponseTimeHours;
        }
        
        @Override
        public Double getOrderFulfillmentRate() {
            return orderFulfillmentRate;
        }
        
        @Override
        public Double getComplaintRate() {
            return complaintRate;
        }
        
        @Override
        public Double getOnTimeDeliveryRate() {
            return onTimeDeliveryRate;
        }
    }
    
    /**
     * Inner class implementation of ArtisanCustomerAnalytics interface
     * Used to combine results from multiple queries
     */
    private static class ArtisanCustomerAnalyticsImpl implements ArtisanCustomerAnalytics {
        private final Long totalCustomers;
        private final Double repeatCustomerRate;
        private final Double customerSatisfactionScore;
        
        public ArtisanCustomerAnalyticsImpl(Long totalCustomers, Double repeatCustomerRate,
                                           Double customerSatisfactionScore) {
            this.totalCustomers = totalCustomers != null ? totalCustomers : 0L;
            this.repeatCustomerRate = repeatCustomerRate;
            this.customerSatisfactionScore = customerSatisfactionScore;
        }
        
        @Override
        public Long getTotalCustomers() {
            return totalCustomers;
        }
        
        @Override
        public Double getRepeatCustomerRate() {
            return repeatCustomerRate;
        }
        
        @Override
        public Double getCustomerSatisfactionScore() {
            return customerSatisfactionScore;
        }
    }
    
    /**
     * Inner class implementation of ArtisanTemplatePerformance interface
     * Used to combine results from multiple queries
     */
    private static class ArtisanTemplatePerformanceImpl implements ArtisanTemplatePerformance {
        private final Long totalTemplates;
        private final Double templateConversionRate;
        
        public ArtisanTemplatePerformanceImpl(Long totalTemplates, Double templateConversionRate) {
            this.totalTemplates = totalTemplates != null ? totalTemplates : 0L;
            this.templateConversionRate = templateConversionRate;
        }
        
        @Override
        public Long getTotalTemplates() {
            return totalTemplates;
        }
        
        @Override
        public Double getTemplateConversionRate() {
            return templateConversionRate;
        }
    }
    
    // ==================== NEW: Admin Financial Metrics ====================
    
    @Override
    public RefundStatistics getRefundStatistics(LocalDateTime startDate) {
        // Get refund stats from RefundTransactionRepository
        Long totalRefunds = refundTransactionRepository.countByCreatedAtAfter(startDate);
        BigDecimal totalRefundAmount = refundTransactionRepository.sumAmountByCreatedAtAfter(startDate);
        
        // Get cancellation stats from CustomOrderRepository
        Long totalCancellations = customOrderRepository.countCancellationsByCreatedAtAfter(startDate);
        BigDecimal totalCancellationAmount = customOrderRepository.sumCancellationAmountByCreatedAtAfter(startDate);
        
        // Calculate refund rate
        Long totalOrders = orderRepository.countByCreatedAtAfter(startDate);
        Double refundRate = null;
        if (totalOrders != null && totalOrders > 0) {
            refundRate = (totalRefunds.doubleValue() / totalOrders.doubleValue()) * 100;
        }
        
        return new RefundStatisticsImpl(
            totalRefunds,
            totalRefundAmount != null ? totalRefundAmount : BigDecimal.ZERO,
            totalCancellations,
            totalCancellationAmount != null ? totalCancellationAmount : BigDecimal.ZERO,
            refundRate
        );
    }
    
    @Override
    public WithdrawalStatistics getWithdrawalStatistics(LocalDateTime startDate) {
        // Get withdrawal stats from WithdrawalRequestRepository
        Long totalRequests = withdrawalRequestRepository.countByCreatedAtAfter(startDate);
        Long pendingRequests = withdrawalRequestRepository.countPendingRequests();
        Long approvedRequests = withdrawalRequestRepository.countApprovedByCreatedAtAfter(startDate);
        Long rejectedRequests = withdrawalRequestRepository.countRejectedByCreatedAtAfter(startDate);
        BigDecimal totalWithdrawnAmount = withdrawalRequestRepository.sumApprovedAmountByCreatedAtAfter(startDate);
        BigDecimal pendingWithdrawalAmount = withdrawalRequestRepository.sumPendingAmount();
        
        return new WithdrawalStatisticsImpl(
            totalRequests != null ? totalRequests : 0L,
            pendingRequests != null ? pendingRequests : 0L,
            approvedRequests != null ? approvedRequests : 0L,
            rejectedRequests != null ? rejectedRequests : 0L,
            totalWithdrawnAmount != null ? totalWithdrawnAmount : BigDecimal.ZERO,
            pendingWithdrawalAmount != null ? pendingWithdrawalAmount : BigDecimal.ZERO
        );
    }
    
    @Override
    public PlatformFinancials getPlatformFinancials(LocalDateTime startDate) {
        // Get admin wallet balance (total platform revenue accumulated)
        BigDecimal adminWalletBalance = walletRepository.getAdminWalletBalance();
        
        return new PlatformFinancialsImpl(
            adminWalletBalance != null ? adminWalletBalance : BigDecimal.ZERO
        );
    }
    
    // ==================== NEW: Artisan Refund Impact ====================
    
    @Override
    public ArtisanRefundImpact getArtisanRefundImpact(UUID artisanId, LocalDateTime startDate) {
        // Get refund stats for this artisan
        Long totalRefunds = refundTransactionRepository.countByArtisanAndCreatedAtAfter(artisanId, startDate);
        BigDecimal totalRefundAmount = refundTransactionRepository.sumAmountByArtisanAndCreatedAtAfter(artisanId, startDate);
        
        // Get cancellation stats for this artisan
        Long totalCancellations = customOrderRepository.countCancellationsByArtisanAndCreatedAtAfter(artisanId, startDate);
        BigDecimal totalCancellationAmount = customOrderRepository.sumCancellationAmountByArtisanAndCreatedAtAfter(artisanId, startDate);
        
        // Get current locked and available balance
        BigDecimal lockedBalance = walletRepository.getLockedBalanceByArtisanId(artisanId);
        BigDecimal availableBalance = walletRepository.getAvailableBalanceByArtisanId(artisanId);
        
        return new ArtisanRefundImpactImpl(
            totalRefunds != null ? totalRefunds : 0L,
            totalRefundAmount != null ? totalRefundAmount : BigDecimal.ZERO,
            totalCancellations != null ? totalCancellations : 0L,
            totalCancellationAmount != null ? totalCancellationAmount : BigDecimal.ZERO,
            lockedBalance != null ? lockedBalance : BigDecimal.ZERO,
            availableBalance != null ? availableBalance : BigDecimal.ZERO
        );
    }
    
    // ==================== NEW: Implementation Classes ====================
    
    private static class RefundStatisticsImpl implements RefundStatistics {
        private final Long totalRefunds;
        private final BigDecimal totalRefundAmount;
        private final Long totalCancellations;
        private final BigDecimal totalCancellationAmount;
        private final Double refundRate;
        
        public RefundStatisticsImpl(Long totalRefunds, BigDecimal totalRefundAmount,
                                   Long totalCancellations, BigDecimal totalCancellationAmount,
                                   Double refundRate) {
            this.totalRefunds = totalRefunds != null ? totalRefunds : 0L;
            this.totalRefundAmount = totalRefundAmount;
            this.totalCancellations = totalCancellations != null ? totalCancellations : 0L;
            this.totalCancellationAmount = totalCancellationAmount;
            this.refundRate = refundRate;
        }
        
        @Override
        public Long getTotalRefunds() {
            return totalRefunds;
        }
        
        @Override
        public BigDecimal getTotalRefundAmount() {
            return totalRefundAmount;
        }
        
        @Override
        public Long getTotalCancellations() {
            return totalCancellations;
        }
        
        @Override
        public BigDecimal getTotalCancellationAmount() {
            return totalCancellationAmount;
        }
        
        @Override
        public Double getRefundRate() {
            return refundRate;
        }
    }
    
    private static class WithdrawalStatisticsImpl implements WithdrawalStatistics {
        private final Long totalRequests;
        private final Long pendingRequests;
        private final Long approvedRequests;
        private final Long rejectedRequests;
        private final BigDecimal totalWithdrawnAmount;
        private final BigDecimal pendingWithdrawalAmount;
        
        public WithdrawalStatisticsImpl(Long totalRequests, Long pendingRequests,
                                       Long approvedRequests, Long rejectedRequests,
                                       BigDecimal totalWithdrawnAmount, BigDecimal pendingWithdrawalAmount) {
            this.totalRequests = totalRequests;
            this.pendingRequests = pendingRequests;
            this.approvedRequests = approvedRequests;
            this.rejectedRequests = rejectedRequests;
            this.totalWithdrawnAmount = totalWithdrawnAmount;
            this.pendingWithdrawalAmount = pendingWithdrawalAmount;
        }
        
        @Override
        public Long getTotalRequests() {
            return totalRequests;
        }
        
        @Override
        public Long getPendingRequests() {
            return pendingRequests;
        }
        
        @Override
        public Long getApprovedRequests() {
            return approvedRequests;
        }
        
        @Override
        public Long getRejectedRequests() {
            return rejectedRequests;
        }
        
        @Override
        public BigDecimal getTotalWithdrawnAmount() {
            return totalWithdrawnAmount;
        }
        
        @Override
        public BigDecimal getPendingWithdrawalAmount() {
            return pendingWithdrawalAmount;
        }
    }
    
    private static class PlatformFinancialsImpl implements PlatformFinancials {
        private final BigDecimal totalPlatformRevenue;
        
        public PlatformFinancialsImpl(BigDecimal totalPlatformRevenue) {
            this.totalPlatformRevenue = totalPlatformRevenue;
        }
        
        @Override
        public BigDecimal getTotalPlatformRevenue() {
            return totalPlatformRevenue;
        }
    }
    
    private static class ArtisanRefundImpactImpl implements ArtisanRefundImpact {
        private final Long totalRefunds;
        private final BigDecimal totalRefundAmount;
        private final Long totalCancellations;
        private final BigDecimal totalCancellationAmount;
        private final BigDecimal lockedBalance;
        private final BigDecimal availableBalance;
        
        public ArtisanRefundImpactImpl(Long totalRefunds, BigDecimal totalRefundAmount,
                                      Long totalCancellations, BigDecimal totalCancellationAmount,
                                      BigDecimal lockedBalance, BigDecimal availableBalance) {
            this.totalRefunds = totalRefunds;
            this.totalRefundAmount = totalRefundAmount;
            this.totalCancellations = totalCancellations;
            this.totalCancellationAmount = totalCancellationAmount;
            this.lockedBalance = lockedBalance;
            this.availableBalance = availableBalance;
        }
        
        @Override
        public Long getTotalRefunds() {
            return totalRefunds;
        }
        
        @Override
        public BigDecimal getTotalRefundAmount() {
            return totalRefundAmount;
        }
        
        @Override
        public Long getTotalCancellations() {
            return totalCancellations;
        }
        
        @Override
        public BigDecimal getTotalCancellationAmount() {
            return totalCancellationAmount;
        }
        
        @Override
        public BigDecimal getLockedBalance() {
            return lockedBalance;
        }
        
        @Override
        public BigDecimal getAvailableBalance() {
            return availableBalance;
        }
    }
}