# Implementation Plan

- [x] 1. Tạo DTO Interface Projections cho Artisan Statistics





  - Tạo 8 interface projections trong package dto.response.Dashboard
  - Mỗi interface định nghĩa getter methods cho fields cần thiết
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 1.1 Tạo ArtisanFinancialDetails interface


  - Tạo file ArtisanFinancialDetails.java với methods: getGrossEarnings(), getTotalCommission(), getNetEarnings(), getPendingWithdrawal(), getCurrentBalance()
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_


- [x] 1.2 Tạo WalletBalanceTrend interface

  - Tạo file WalletBalanceTrend.java với methods: getDate(), getBalance()
  - _Requirements: 1.6_


- [x] 1.3 Tạo ArtisanCustomOrderStats interface

  - Tạo file ArtisanCustomOrderStats.java với methods: getTotalRequests(), getTotalOrders(), getAvgOrderValue(), getConversionRate(), getPendingRequests(), getAvgCompletionDays()
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_


- [x] 1.4 Tạo ArtisanPerformanceMetrics interface

  - Tạo file ArtisanPerformanceMetrics.java với methods: getAvgRating(), getTotalReviews(), getAvgResponseTimeHours(), getOrderFulfillmentRate(), getComplaintRate(), getOnTimeDeliveryRate()
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_


- [x] 1.5 Tạo ArtisanCustomerAnalytics interface

  - Tạo file ArtisanCustomerAnalytics.java với methods: getTotalCustomers(), getRepeatCustomerRate(), getCustomerSatisfactionScore()
  - _Requirements: 4.1, 4.2, 4.3_


- [x] 1.6 Tạo TopCustomerDTO interface (nếu chưa có)

  - Tạo file TopCustomerDTO.java với methods: getCustomerId(), getCustomerName(), getEmail(), getTotalOrders(), getTotalSpent()
  - _Requirements: 4.4, 4.5_

- [x] 1.7 Tạo ArtisanTemplatePerformance interface


  - Tạo file ArtisanTemplatePerformance.java với methods: getTotalTemplates(), getTemplateConversionRate()
  - _Requirements: 5.1, 5.4_


- [x] 1.8 Tạo TopTemplateDTO interface

  - Tạo file TopTemplateDTO.java với methods: getTemplateId(), getTemplateName(), getTotalOrders(), getTotalRevenue()
  - _Requirements: 5.2, 5.3_
-

- [x] 2. Cập nhật DashboardResponse class




  - Thêm 7 fields mới vào DashboardResponse class cho artisan-specific stats
  - Fields: financialDetails, artisanCustomOrderStats, performanceMetrics, customerAnalytics, templatePerformance, artisanTopCustomers, topTemplates
  - _Requirements: 6.5, 6.6_



- [x] 3. Thêm Repository Query Methods



  - Thêm query methods vào các repositories hiện có và tạo repository mới nếu cần
  - Sử dụng JPQL với @Query annotation
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_

- [x] 3.1 Thêm methods vào WalletTransactionRepository


  - Thêm method getFinancialSummary(@Param("artisanId") UUID artisanId, @Param("startDate") LocalDateTime startDate)
  - Query tính grossEarnings từ PAYMENT_RECEIVED và STAGE_PAYMENT_RECEIVED, tổng commissionFee
  - Thêm method getWalletBalanceTrend(@Param("artisanId") UUID artisanId, @Param("startDate") LocalDateTime startDate)
  - Query tổng hợp WalletTransaction theo ngày, tính cumulative balance
  - Thêm method getCurrentBalance(@Param("artisanId") UUID artisanId)
  - Query lấy balanceAfter của transaction mới nhất
  - _Requirements: 1.1, 1.2, 1.3, 1.6, 7.1, 7.2, 7.3_


- [x] 3.2 Thêm method vào WithdrawalRequestRepository

  - Thêm method getPendingWithdrawalAmount(@Param("artisanId") UUID artisanId)
  - Query tổng amount từ WithdrawalRequest với status = PENDING
  - _Requirements: 1.5_


- [x] 3.3 Thêm methods vào CustomRequestRepository

  - Thêm method getCustomOrderStats(@Param("artisanId") UUID artisanId, @Param("startDate") LocalDateTime startDate)
  - Query đếm totalRequests, totalOrders (có customOrder), tính avgOrderValue và conversionRate
  - Thêm method getPendingRequestsCount(@Param("artisanId") UUID artisanId)
  - Query đếm CustomRequest với status IN ('PENDING', 'QUOTED')
  - _Requirements: 2.1, 2.2, 2.3, 2.5, 7.4, 7.6, 7.7_


- [x] 3.4 Thêm method vào CustomOrderRepository

  - Thêm method getAvgCompletionTimeDays(@Param("artisanId") UUID artisanId, @Param("startDate") LocalDateTime startDate)
  - Query tính AVG(DATEDIFF(completedAt, createdAt)) cho CustomOrder với status = COMPLETED
  - _Requirements: 2.4_


- [x] 3.5 Thêm method vào FeedbackRepository

  - Thêm method getRatingStats(@Param("artisanId") UUID artisanId)
  - Query tính AVG(rating) và COUNT(feedback) cho artisan
  - _Requirements: 3.1, 3.2, 7.6_


- [x] 3.6 Tạo hoặc cập nhật ConversationRepository

  - Thêm method getAvgResponseTimeHours(@Param("artisanId") UUID artisanId)
  - Query tính AVG(TIMESTAMPDIFF(HOUR, customRequest.createdAt, MIN(chatMessage.createdAt)))
  - Chỉ tính cho messages từ artisan trong conversation của customRequest
  - _Requirements: 3.3, 7.6_


- [x] 3.7 Thêm methods vào OrderRepository

  - Thêm method getOrderFulfillmentRate(@Param("artisanId") UUID artisanId)
  - Query tính (COUNT(status=DELIVERED) * 100.0 / COUNT(status!=CANCELLED)) cho orders của artisan
  - Thêm method getOnTimeDeliveryRate(@Param("artisanId") UUID artisanId)
  - Query tính (COUNT(actualDeliveryDate <= expectedDeliveryDate) * 100.0 / COUNT(status=DELIVERED))
  - _Requirements: 3.4, 3.6, 7.7_


- [x] 3.8 Thêm method vào ComplaintRepository

  - Thêm method getComplaintRate(@Param("artisanId") UUID artisanId, @Param("totalOrders") Long totalOrders)
  - Query tính (COUNT(complaints) * 100.0 / totalOrders) cho complaints liên quan đến artisan
  - _Requirements: 3.5, 7.7_


- [x] 3.9 Thêm methods vào AccountRepository

  - Thêm method getArtisanTopCustomers(@Param("artisanId") UUID artisanId, @Param("startDate") LocalDateTime startDate, Pageable pageable)
  - Query JOIN Account với Order và CustomOrder, GROUP BY customer, ORDER BY totalSpent DESC, limit 10
  - Thêm method getCustomerStats(@Param("artisanId") UUID artisanId, @Param("startDate") LocalDateTime startDate)
  - Query đếm DISTINCT customers, tính repeatRate (customers với orderCount > 1)
  - _Requirements: 4.1, 4.2, 4.4, 4.5, 7.2_


- [x] 3.10 Tạo ProductTemplateRepository (nếu chưa có)

  - Thêm method getTotalTemplatesCount(@Param("artisanId") UUID artisanId)
  - Query đếm ProductTemplate của artisan
  - Thêm method getTopTemplates(@Param("artisanId") UUID artisanId, Pageable pageable)
  - Query JOIN ProductTemplate với OrderTemplateDetail, GROUP BY template, ORDER BY totalOrders DESC, limit 10
  - Thêm method getTemplateConversionRate(@Param("artisanId") UUID artisanId)
  - Query tính (COUNT(templates có orders) * 100.0 / COUNT(all templates))
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 7.7_

- [x] 4. Cập nhật DashboardService Interface




  - Thêm 8 method signatures mới vào DashboardService interface
  - Methods cho artisan: getArtisanFinancialDetails(), getWalletBalanceTrend(), getArtisanCustomOrderStats(), getArtisanPerformanceMetrics(), getArtisanCustomerAnalytics(), getArtisanTopCustomers(), getArtisanTemplatePerformance(), getArtisanTopTemplates()
  - Cập nhật method getArtisanDashboardInDays() signature
  - _Requirements: 1.1, 1.6, 2.1, 3.1, 4.1, 4.4, 5.1, 5.2, 6.1, 6.2, 6.3, 6.4, 6.5_


- [x] 5. Implement DashboardServiceImp Methods


  - Thêm dependencies mới vào DashboardServiceImp
  - Implement 8 methods mới cho artisan statistics
  - Cập nhật method getArtisanDashboardInDays() để include statistics mới
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_


- [x] 5.1 Thêm dependencies vào DashboardServiceImp

  - Inject WalletTransactionRepository, WithdrawalRequestRepository, ConversationRepository (nếu có), ProductTemplateRepository
  - Các repositories khác đã có sẵn từ admin dashboard
  - _Requirements: 6.1_


- [x] 5.2 Implement getArtisanFinancialDetails() method

  - Gọi walletTransactionRepository.getFinancialSummary(artisanId, startDate)
  - Gọi withdrawalRequestRepository.getPendingWithdrawalAmount(artisanId)
  - Gọi walletTransactionRepository.getCurrentBalance(artisanId)
  - Tạo ArtisanFinancialDetailsImpl inner class để kết hợp kết quả
  - Tính netEarnings = grossEarnings - totalCommission
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 7.1_


- [x] 5.3 Implement getWalletBalanceTrend() method
  - Gọi walletTransactionRepository.getWalletBalanceTrend(artisanId, startDate)
  - Return kết quả trực tiếp
  - _Requirements: 1.6, 1.7_


- [x] 5.4 Implement getArtisanCustomOrderStats() method
  - Gọi customRequestRepository.getCustomOrderStats(artisanId, startDate)
  - Gọi customRequestRepository.getPendingRequestsCount(artisanId)
  - Gọi customOrderRepository.getAvgCompletionTimeDays(artisanId, startDate)
  - Tạo ArtisanCustomOrderStatsImpl inner class để kết hợp kết quả

  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 7.6, 7.7_

- [x] 5.5 Implement getArtisanPerformanceMetrics() method
  - Gọi feedbackRepository.getRatingStats(artisanId)
  - Gọi conversationRepository.getAvgResponseTimeHours(artisanId) (nếu có)
  - Gọi orderRepository.getOrderFulfillmentRate(artisanId)
  - Gọi orderRepository.getOnTimeDeliveryRate(artisanId)
  - Tính totalOrders từ orderRepository
  - Gọi complaintRepository.getComplaintRate(artisanId, totalOrders)

  - Tạo ArtisanPerformanceMetricsImpl inner class để kết hợp kết quả
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 7.6, 7.7_

- [x] 5.6 Implement getArtisanCustomerAnalytics() method
  - Gọi accountRepository.getCustomerStats(artisanId, startDate)
  - Gọi feedbackRepository.getRatingStats(artisanId) để tính satisfaction score

  - Tính customerSatisfactionScore = (avgRating / 5.0) * 100
  - Tạo ArtisanCustomerAnalyticsImpl inner class để kết hợp kết quả
  - _Requirements: 4.1, 4.2, 4.3, 7.6_


- [x] 5.7 Implement getArtisanTopCustomers() method
  - Gọi accountRepository.getArtisanTopCustomers(artisanId, startDate, PageRequest.of(0, 10))
  - Return kết quả trực tiếp
  - _Requirements: 4.4, 4.5_


- [x] 5.8 Implement getArtisanTemplatePerformance() method
  - Gọi productTemplateRepository.getTotalTemplatesCount(artisanId)
  - Gọi productTemplateRepository.getTemplateConversionRate(artisanId)
  - Tạo ArtisanTemplatePerformanceImpl inner class để kết hợp kết quả

  - _Requirements: 5.1, 5.4, 7.7_

- [x] 5.9 Implement getArtisanTopTemplates() method
  - Gọi productTemplateRepository.getTopTemplates(artisanId, PageRequest.of(0, 10))
  - Return kết quả trực tiếp
  - _Requirements: 5.2, 5.3_

- [x] 5.10 Cập nhật getArtisanDashboardInDays() method


  - Giữ nguyên authorization check hiện có (ARTISAN role)
  - Giữ nguyên các method calls hiện có (summary, revenueChart, orderStatus, topProducts, lowStockProducts)
  - Thêm 7 method calls mới vào DashboardResponse.builder()
  - Thêm .financialDetails(getArtisanFinancialDetails(artisanId, start))
  - Thêm .artisanCustomOrderStats(getArtisanCustomOrderStats(artisanId, start))
  - Thêm .performanceMetrics(getArtisanPerformanceMetrics(artisanId, start))
  - Thêm .customerAnalytics(getArtisanCustomerAnalytics(artisanId, start))
  - Thêm .templatePerformance(getArtisanTemplatePerformance(artisanId, start))
  - Thêm .artisanTopCustomers(getArtisanTopCustomers(artisanId, start))
  - Thêm .topTemplates(getArtisanTopTemplates(artisanId, start))
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_

- [ ] 6. Tạo DashboardController
  - Tạo DashboardController mới với 2 endpoints: /api/dashboard/admin và /api/dashboard/artisan
  - Sử dụng @PreAuthorize("hasAuthority('ADMIN')") cho admin endpoint
  - Sử dụng @PreAuthorize("hasAuthority('ARTISAN')") cho artisan endpoint
  - Inject DashboardService và AccountService
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_

- [ ] 6.1 Tạo DashboardController class
  - Tạo file DashboardController.java trong package controller
  - Thêm @RestController, @RequestMapping("/api/dashboard"), @RequiredArgsConstructor annotations
  - Inject DashboardService và AccountService dependencies
  - _Requirements: 6.1, 6.2_

- [ ] 6.2 Implement getAdminDashboard() endpoint
  - Tạo method getAdminDashboard(@RequestParam(defaultValue = "30") int days, Authentication authentication)
  - Thêm @GetMapping("/admin") và @PreAuthorize("hasAuthority('ADMIN')")
  - Lấy adminId từ authentication
  - Gọi dashboardService.getAdminDashboardInDays(adminId, days)
  - Return ResponseEntity.ok(BaseResponse.success(response))
  - _Requirements: 6.1, 6.2, 6.3, 6.6_

- [ ] 6.3 Implement getArtisanDashboard() endpoint
  - Tạo method getArtisanDashboard(@RequestParam(defaultValue = "30") int days, Authentication authentication)
  - Thêm @GetMapping("/artisan") và @PreAuthorize("hasAuthority('ARTISAN')")
  - Lấy artisanId từ authentication
  - Gọi dashboardService.getArtisanDashboardInDays(artisanId, days)
  - Return ResponseEntity.ok(BaseResponse.success(response))
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_
-

- [x] 7. Verify và Test Implementation





  - Compile code để check syntax errors
  - Test API endpoint với Postman/curl
  - Verify response structure và data accuracy
  - _Requirements: 6.8, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_


- [x] 7.1 Compile và check diagnostics

  - Run build để verify không có compilation errors
  - Check tất cả imports đúng
  - Verify không có type mismatches
  - _Requirements: 7.1, 7.2_


- [x] 7.2 Test Artisan Dashboard API endpoint



  - Start application
  - Gọi GET /api/dashboard/artisan?days=30 với artisan token
  - Verify response có tất cả 7 statistics mới (financialDetails, artisanCustomOrderStats, performanceMetrics, customerAnalytics, templatePerformance, artisanTopCustomers, topTemplates)
  - Check data types và values hợp lý
  - Verify null handling cho edge cases
  - _Requirements: 6.8, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_
