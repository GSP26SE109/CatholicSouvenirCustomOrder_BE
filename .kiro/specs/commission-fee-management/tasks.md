# Implementation Plan - Hệ Thống Quản Lý Phí Sàn

## Tổng Quan

Danh sách tasks để triển khai hệ thống quản lý phí sàn. Các tasks được sắp xếp theo thứ tự ưu tiên và phụ thuộc lẫn nhau.

## Tasks

- [x] 1. Tạo và cập nhật entity classes (JPA Code-First)







  - Tạo SystemConfig entity với @Entity annotation
  - Cập nhật Payment entity thêm field commissionRate
  - Cập nhật StagePayment entity thêm field commissionRate
  - Cập nhật WalletTransaction entity thêm fields commissionFee và commissionRate
  - Thêm @Index annotations cho performance optimization
  - JPA sẽ tự động tạo/update database schema khi application start
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

- [x] 2. Tạo model classes và repositories




- [x] 2.1 Tạo SystemConfig entity

  - Implement SystemConfig.java với các fields: configKey, configValue, description, updatedAt, updatedBy
  - Thêm @PreUpdate hook để tự động cập nhật updatedAt
  - _Requirements: 11.5_


- [x] 2.2 Cập nhật Payment entity

  - Thêm field commissionRate (BigDecimal) vào Payment.java
  - Set default value = BigDecimal.ZERO
  - _Requirements: 11.1_



- [x] 2.3 Cập nhật StagePayment entity

  - Thêm field commissionRate (BigDecimal) vào StagePayment.java
  - Set default value = BigDecimal.ZERO
  - _Requirements: 11.2_




- [x] 2.4 Cập nhật WalletTransaction entity

  - Thêm fields commissionFee và commissionRate vào WalletTransaction.java
  - Set default commissionFee = BigDecimal.ZERO

  - _Requirements: 11.3, 11.4_




- [x] 2.5 Tạo SystemConfigRepository

  - Extend JpaRepository<SystemConfig, String>
  - Thêm method findByConfigKey nếu cần
  - _Requirements: 1.1_

- [x] 3. Implement SystemConfigService





- [x] 3.1 Tạo SystemConfigService interface và implementation


  - Implement method getCommissionRate() với Redis cache
  - Implement method updateCommissionRate() với time window validation
  - Implement method getConfig() để lấy config theo key
  - Cache commission_rate trong Redis với TTL 24 giờ
  - _Requirements: 1.1, 1.2, 1.3, 1.6, 3.1_

- [x] 3.2 Implement time window validation


  - Validate thời gian hiện tại phải từ 00:00-00:59
  - Throw InvalidTimeWindowException nếu ngoài khung giờ
  - _Requirements: 1.3, 2.3_

- [x] 3.3 Implement commission rate validation


  - Validate giá trị từ 0 đến 100
  - Throw BusinessException nếu không hợp lệ
  - _Requirements: 1.2, 2.4_

- [x] 3.4 Implement logging cho commission rate changes


  - Log timestamp, giá trị cũ, giá trị mới, admin ID
  - _Requirements: 1.5_

- [x] 4. Implement CommissionService





- [x] 4.1 Tạo CommissionService interface và implementation


  - Implement method calculateCommission(amount, rate)
  - Implement method applyCommissionToWalletTransaction()
  - Tạo CommissionCalculation DTO để return kết quả tính toán
  - _Requirements: 4.2, 4.3, 5.2, 5.3_

- [x] 4.2 Implement commission calculation logic


  - Tính commission_amount = amount × rate / 100
  - Tính net_amount = amount - commission_amount
  - Làm tròn đến 2 chữ số thập phân
  - Validate net_amount > 0
  - _Requirements: 4.2, 4.7, 9.6_

- [x] 4.3 Implement commission application logic


  - Set commissionFee và commissionRate vào WalletTransaction
  - Log thông tin commission đã áp dụng
  - _Requirements: 4.5, 9.4_

- [x] 5. Cập nhật PaymentService





- [x] 5.1 Cập nhật method createPayment()


  - Tự động lấy commission_rate từ SystemConfigService
  - Lưu commission_rate vào Payment entity
  - Log commission_rate được sử dụng
  - _Requirements: 3.1, 3.2, 3.7_

- [x] 5.2 Cập nhật method handlePaymentCallback()


  - Lấy commission_rate từ Payment entity (đã lưu trước đó)
  - Gọi CommissionService.calculateCommission()
  - Cộng NET AMOUNT (đã trừ commission) vào wallet
  - Gọi CommissionService.applyCommissionToWalletTransaction()
  - Đảm bảo logic trong cùng @Transactional
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.8_

- [x] 5.3 Gửi notification cho artisan về commission


  - Gọi NotificationService với type COMMISSION_DEDUCTED
  - Bao gồm thông tin: order_id, original_amount, commission_amount, net_amount
  - _Requirements: 7.1, 7.2, 7.3, 7.5_

- [x] 6. Cập nhật StagePaymentService






- [x] 6.1 Cập nhật method createStagePayment()

  - Tự động lấy commission_rate từ SystemConfigService
  - Lưu commission_rate vào StagePayment entity
  - _Requirements: 3.1, 3.3_


- [x] 6.2 Cập nhật method handleStagePaymentCallback()

  - Lấy commission_rate từ StagePayment entity
  - Tính commission và cộng net amount vào wallet
  - Apply commission vào wallet transaction
  - Gửi notification cho artisan
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.7_

- [x] 7. Implement CommissionController





- [x] 7.1 Tạo endpoint GET /api/commission/rate


  - Public endpoint cho authenticated users
  - Trả về commission_rate hiện tại
  - _Requirements: 10.1, 10.2_

- [x] 7.2 Tạo endpoint GET /api/admin/commission/config


  - Admin-only endpoint
  - Trả về commission_rate, updatedAt, updatedBy, allowedUpdateTime
  - _Requirements: 1.4, 2.1_

- [x] 7.3 Tạo endpoint PUT /api/admin/commission/config


  - Admin-only endpoint
  - Validate time window và commission rate
  - Gọi SystemConfigService.updateCommissionRate()
  - Clear Redis cache sau khi update
  - _Requirements: 1.3, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 7.4 Tạo endpoint GET /api/admin/commission/report


  - Admin-only endpoint
  - Query params: startDate, endDate, groupBy
  - Trả về total_commission, total_transactions, average_commission
  - Group theo DAY/WEEK/MONTH
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 8. Implement CommissionReportService





- [x] 8.1 Tạo CommissionReportService interface và implementation


  - Implement method generateReport(startDate, endDate, groupBy)
  - Query wallet_transactions với commission_fee > 0
  - Tính tổng commission, số lượng transactions, trung bình
  - Group data theo ngày/tuần/tháng
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_



- [x] 9. Implement DTOs




- [x] 9.1 Tạo request DTOs

  - UpdateCommissionRateRequest với validation @DecimalMin, @DecimalMax
  - _Requirements: 2.4_


- [x] 9.2 Tạo response DTOs

  - CommissionRateResponse
  - CommissionConfigResponse
  - CommissionReportResponse
  - CommissionReportItem
  - _Requirements: 2.1, 8.3, 10.2_


- [x] 9.3 Tạo CommissionCalculation DTO

  - Fields: originalAmount, commissionAmount, netAmount
  - _Requirements: 4.2, 4.3_

- [x] 10. Cập nhật WalletService






- [x] 10.1 Cập nhật WalletTransactionResponse

  - Thêm fields: commissionFee, commissionRate
  - Thêm calculated field: originalAmount (amount + commissionFee)
  - Format commission_fee với 2 chữ số thập phân và VND
  - _Requirements: 6.1, 6.2, 6.5_


- [x] 10.2 Cập nhật method getWalletTransactionHistory()

  - Include commission_fee và commission_rate trong response
  - Hiển thị commission_fee = 0 cho transactions không có commission
  - _Requirements: 6.1, 6.4_

- [x] 11. Implement notification cho commission




- [x] 11.1 Thêm NotificationType.COMMISSION_DEDUCTED

  - Thêm enum value vào NotificationType
  - _Requirements: 7.3_


- [x] 11.2 Implement notifyArtisanCommissionDeducted()





  - Tạo notification với thông tin: order_id, original_amount, commission_amount, net_amount
  - Link đến wallet transaction detail
  - _Requirements: 7.1, 7.2, 7.4_


- [x] 11.3 Implement notifyAllArtisansCommissionChange()




  - Gửi notification cho tất cả artisans khi admin thay đổi commission_rate
  - Bao gồm giá trị cũ và mới
  - _Requirements: 10.4_

- [x] 12. Implement error handling




- [x] 12.1 Tạo exception classes


  - InvalidTimeWindowException
  - CommissionCalculationException
  - _Requirements: 2.3, 9.3_

- [x] 12.2 Cập nhật GlobalExceptionHandler


  - Handle InvalidTimeWindowException → 400 Bad Request
  - Handle CommissionCalculationException → 500 Internal Server Error
  - _Requirements: 2.3_

- [x] 13. Implement refund logic với commission





- [x] 13.1 Cập nhật RefundService


  - Khi refund payment, hoàn lại TOÀN BỘ original_amount (bao gồm commission)
  - Tạo wallet transaction với commission_fee âm để hoàn commission
  - _Requirements: 9.2_

- [ ] 14. Testing và validation
- [ ]* 14.1 Viết unit tests cho SystemConfigService
  - Test getCommissionRate() với cache hit/miss
  - Test updateCommissionRate() với time window validation
  - Test commission rate validation (0-100)
  - _Requirements: 1.2, 1.3, 1.6_

- [ ]* 14.2 Viết unit tests cho CommissionService
  - Test calculateCommission() với nhiều scenarios
  - Test edge cases: rate=0, rate=100, amount=0
  - Test rounding logic
  - _Requirements: 4.2, 4.7, 9.1, 9.6_

- [ ]* 14.3 Viết integration tests cho payment flow
  - Test toàn bộ flow: create payment → callback → wallet update
  - Verify commission được tính và trừ đúng
  - Verify notification được gửi
  - _Requirements: 3.1, 3.2, 4.1, 4.2, 4.3, 4.4, 4.5, 7.1_

- [ ]* 14.4 Viết integration tests cho stage payment flow
  - Test flow: create stage payment → callback → wallet update
  - Verify commission cho stage payment
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 14.5 Test commission report generation
  - Test với nhiều date ranges
  - Test groupBy DAY/WEEK/MONTH
  - Verify calculations
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [ ] 15. Documentation và deployment
- [ ] 15.1 Tạo API documentation
  - Document tất cả commission endpoints
  - Thêm examples cho request/response
  - Document error codes

- [ ] 15.2 Insert default commission rate data
  - Tạo @PostConstruct method hoặc data initialization script
  - Insert default commission_rate = 5.00 vào system_config nếu chưa tồn tại
  - _Requirements: 3.5_

- [ ] 15.3 Cập nhật README
  - Thêm thông tin về commission system
  - Hướng dẫn config commission rate
  - FAQ về commission
