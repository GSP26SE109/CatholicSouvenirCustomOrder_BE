# Tài Liệu Thiết Kế - Hệ Thống Quản Lý Phí Sàn

## Tổng Quan

Hệ thống quản lý phí sàn được thiết kế để tự động thu phí từ các giao dịch thanh toán của nghệ nhân. Thiết kế tập trung vào tính đơn giản, không ảnh hưởng đến logic payment hiện tại, và đảm bảo tính nhất quán của commission rate.

## Kiến Trúc

### Nguyên Tắc Thiết Kế

1. **Snapshot Pattern**: Commission rate được "snapshot" vào Payment/StagePayment khi tạo
2. **Single Source of Truth**: SystemConfig là nguồn duy nhất cho commission rate
3. **Non-Invasive**: Không thay đổi logic payment callback hiện tại, chỉ thêm logic tính commission
4. **Cache-First**: Sử dụng Redis cache để tối ưu performance

### Luồng Dữ Liệu

```
Admin Config (00:00-00:59) → SystemConfig Table → Redis Cache (24h TTL)
                                                          ↓
Customer Checkout → Payment Entity (snapshot commission_rate từ cache)
                                                          ↓
Payment Gateway → Callback → Calculate Commission → Update Wallet
                                                          ↓
                                    Wallet Transaction (với commission_fee)
```

## Các Thành Phần

### 1. Database Schema

#### Bảng Mới: system_config

```sql
CREATE TABLE system_config (
    config_key VARCHAR(255) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description VARCHAR(500),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by UUID,
    FOREIGN KEY (updated_by) REFERENCES accounts(account_id)
);

-- Insert default commission rate
INSERT INTO system_config (config_key, config_value, description) 
VALUES ('PLATFORM_COMMISSION_RATE', '5.00', 'Tỷ lệ phí sàn (%) thu từ giao dịch nghệ nhân');
```

#### Cập Nhật Bảng Hiện Tại

```sql
-- Thêm commission_rate vào payments
ALTER TABLE payments 
ADD COLUMN commission_rate DECIMAL(5,2) DEFAULT 0 COMMENT 'Tỷ lệ phí sàn tại thời điểm tạo payment (%)';

-- Thêm commission_rate vào stage_payments
ALTER TABLE stage_payments 
ADD COLUMN commission_rate DECIMAL(5,2) DEFAULT 0 COMMENT 'Tỷ lệ phí sàn tại thời điểm tạo stage payment (%)';

-- Thêm commission fields vào wallet_transactions
ALTER TABLE wallet_transactions 
ADD COLUMN commission_fee DECIMAL(15,2) DEFAULT 0 COMMENT 'Số tiền phí sàn bị trừ (VND)',
ADD COLUMN commission_rate DECIMAL(5,2) COMMENT 'Tỷ lệ phí sàn được áp dụng (%)';

-- Tạo index cho báo cáo
CREATE INDEX idx_wallet_transactions_commission ON wallet_transactions(commission_fee, created_at);
```

### 2. Model Classes

#### SystemConfig.java (Mới)

```java
package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "system_config")
public class SystemConfig {
    
    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;
    
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "updated_by")
    private Account updatedBy;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### Cập Nhật Payment.java

```java
// Thêm vào class Payment hiện tại
@Column(name = "commission_rate", precision = 5, scale = 2)
private BigDecimal commissionRate = BigDecimal.ZERO;
```

#### Cập Nhật StagePayment.java

```java
// Thêm vào class StagePayment hiện tại
@Column(name = "commission_rate", precision = 5, scale = 2)
private BigDecimal commissionRate = BigDecimal.ZERO;
```

#### Cập Nhật WalletTransaction.java

```java
// Thêm vào class WalletTransaction hiện tại (sau field description)
@Column(name = "commission_fee", precision = 18, scale = 2)
private BigDecimal commissionFee = BigDecimal.ZERO;

@Column(name = "commission_rate", precision = 5, scale = 2)
private BigDecimal commissionRate;
```

### 3. Service Layer

#### SystemConfigService.java (Mới)

```java
public interface SystemConfigService {
    BigDecimal getCommissionRate();
    void updateCommissionRate(BigDecimal newRate, UUID adminId);
    SystemConfig getConfig(String key);
}
```

#### SystemConfigServiceImp.java (Mới)

```java
package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.SystemConfig;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.SystemConfigRepository;
import org.example.catholicsouvenircustomorder.service.NotificationService;
import org.example.catholicsouvenircustomorder.service.SystemConfigService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConfigServiceImp implements SystemConfigService {
    
    private static final String COMMISSION_RATE_KEY = "PLATFORM_COMMISSION_RATE";
    private static final String CACHE_KEY = "system:config:commission_rate";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    
    private final SystemConfigRepository systemConfigRepository;
    private final AccountRepository accountRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;
    
    @Override
    public BigDecimal getCommissionRate() {
        // Try cache first
        String cachedValue = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cachedValue != null) {
            log.debug("Commission rate from cache: {}", cachedValue);
            return new BigDecimal(cachedValue);
        }
        
        // Get from database
        SystemConfig config = systemConfigRepository.findById(COMMISSION_RATE_KEY)
            .orElseGet(() -> createDefaultConfig());
        
        BigDecimal rate = new BigDecimal(config.getConfigValue());
        
        // Cache it
        redisTemplate.opsForValue().set(CACHE_KEY, rate.toString(), CACHE_TTL);
        log.info("Commission rate loaded from database and cached: {}", rate);
        
        return rate;
    }
    
    @Override
    @Transactional
    public void updateCommissionRate(BigDecimal newRate, UUID adminId) {
        // Validate time window (00:00-00:59)
        LocalTime now = LocalTime.now();
        if (now.getHour() != 0) {
            throw new BadRequestException("Chỉ có thể cập nhật phí sàn từ 00:00-00:59");
        }
        
        // Validate rate range
        if (newRate.compareTo(BigDecimal.ZERO) < 0 || 
            newRate.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Commission rate phải từ 0 đến 100");
        }
        
        SystemConfig config = systemConfigRepository.findById(COMMISSION_RATE_KEY)
            .orElseThrow(() -> new ResourceNotFoundException("Config not found"));
        
        String oldValue = config.getConfigValue();
        config.setConfigValue(newRate.toString());
        
        Account admin = accountRepository.findById(adminId).orElse(null);
        config.setUpdatedBy(admin);
        
        systemConfigRepository.save(config);
        
        // Clear cache
        redisTemplate.delete(CACHE_KEY);
        
        // Log change
        log.info("Commission rate updated from {} to {} by admin {}", 
            oldValue, newRate, adminId);
        
        // Send notification to all artisans
        notificationService.notifyAllArtisansCommissionChange(oldValue, newRate.toString());
    }
    
    @Override
    public SystemConfig getConfig(String key) {
        return systemConfigRepository.findById(key)
            .orElseThrow(() -> new ResourceNotFoundException("Config not found: " + key));
    }
    
    private SystemConfig createDefaultConfig() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(COMMISSION_RATE_KEY);
        config.setConfigValue("5.00");
        config.setDescription("Tỷ lệ phí sàn (%) thu từ giao dịch nghệ nhân");
        SystemConfig saved = systemConfigRepository.save(config);
        log.info("Created default commission rate config: 5.00%");
        return saved;
    }
}
```

#### CommissionService.java (Mới)

```java
public interface CommissionService {
    CommissionCalculation calculateCommission(BigDecimal amount, BigDecimal rate);
    void applyCommissionToWalletTransaction(WalletTransaction transaction, 
                                           BigDecimal commissionAmount, 
                                           BigDecimal commissionRate);
}
```

#### CommissionServiceImp.java (Mới)

```java
@Service
@Slf4j
public class CommissionServiceImp implements CommissionService {
    
    @Override
    public CommissionCalculation calculateCommission(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            return new CommissionCalculation(amount, BigDecimal.ZERO, amount);
        }
        
        // Calculate commission: amount * rate / 100
        BigDecimal commissionAmount = amount
            .multiply(rate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        
        // Calculate net amount
        BigDecimal netAmount = amount.subtract(commissionAmount);
        
        // Validate
        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Net amount must be greater than 0");
        }
        
        log.info("Commission calculated: amount={}, rate={}%, commission={}, net={}", 
            amount, rate, commissionAmount, netAmount);
        
        return new CommissionCalculation(amount, commissionAmount, netAmount);
    }
    
    @Override
    public void applyCommissionToWalletTransaction(WalletTransaction transaction, 
                                                   BigDecimal commissionAmount, 
                                                   BigDecimal commissionRate) {
        transaction.setCommissionFee(commissionAmount);
        transaction.setCommissionRate(commissionRate);
    }
}

@Data
@AllArgsConstructor
public class CommissionCalculation {
    private BigDecimal originalAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
}
```

### 4. Cập Nhật Payment Service

#### Cập Nhật PaymentServiceImp.java

```java
@Service
@Slf4j
public class PaymentServiceImp implements PaymentService {
    
    private final SystemConfigService systemConfigService;
    private final CommissionService commissionService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    
    // Cập nhật method tạo payment
    @Override
    @Transactional
    public Payment createPayment(Order order) {
        Payment payment = new Payment();
        payment.setAmount(order.getTotal());
        payment.setOrder(order);
        
        // TỰ ĐỘNG lấy commission rate từ config
        BigDecimal commissionRate = systemConfigService.getCommissionRate();
        payment.setCommissionRate(commissionRate);
        
        log.info("Payment created with commission_rate={}%", commissionRate);
        
        return paymentRepository.save(payment);
    }
    
    // Cập nhật method xử lý callback
    @Override
    @Transactional
    public void handlePaymentCallback(String paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> new NotFoundException("Payment not found"));
        
        if (status == PaymentStatus.COMPLETED) {
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            
            // Tính commission từ rate đã lưu trong payment
            CommissionCalculation calc = commissionService.calculateCommission(
                payment.getAmount(), 
                payment.getCommissionRate()
            );
            
            // Cộng NET AMOUNT vào wallet (đã trừ commission)
            Artisan artisan = payment.getOrder().getArtisan();
            WalletTransaction walletTx = walletService.addBalance(
                artisan.getWallet(),
                calc.getNetAmount(),
                "Thanh toán đơn hàng #" + payment.getOrder().getOrderId()
            );
            
            // Ghi commission vào wallet transaction
            commissionService.applyCommissionToWalletTransaction(
                walletTx,
                calc.getCommissionAmount(),
                payment.getCommissionRate()
            );
            
            walletTransactionRepository.save(walletTx);
            
            // Gửi notification
            notificationService.notifyArtisanCommissionDeducted(
                artisan,
                payment.getOrder().getOrderId(),
                calc.getOriginalAmount(),
                calc.getCommissionAmount(),
                calc.getNetAmount()
            );
            
            log.info("Payment {} completed. Original: {}, Commission: {}, Net: {}", 
                paymentId, calc.getOriginalAmount(), 
                calc.getCommissionAmount(), calc.getNetAmount());
        }
    }
}
```

#### Cập Nhật StagePaymentServiceImp.java

```java
@Service
@Slf4j
public class StagePaymentServiceImp implements StagePaymentService {
    
    private final SystemConfigService systemConfigService;
    private final CommissionService commissionService;
    
    @Override
    @Transactional
    public StagePayment createStagePayment(CustomOrderStage stage, BigDecimal amount) {
        StagePayment stagePayment = new StagePayment();
        stagePayment.setAmount(amount);
        stagePayment.setStage(stage);
        
        // TỰ ĐỘNG lấy commission rate
        BigDecimal commissionRate = systemConfigService.getCommissionRate();
        stagePayment.setCommissionRate(commissionRate);
        
        return stagePaymentRepository.save(stagePayment);
    }
    
    @Override
    @Transactional
    public void handleStagePaymentCallback(String stagePaymentId, PaymentStatus status) {
        StagePayment stagePayment = stagePaymentRepository.findById(UUID.fromString(stagePaymentId))
            .orElseThrow(() -> new NotFoundException("Stage payment not found"));
        
        if (status == PaymentStatus.COMPLETED) {
            stagePayment.setStatus(PaymentStatus.COMPLETED);
            stagePaymentRepository.save(stagePayment);
            
            // Tính commission
            CommissionCalculation calc = commissionService.calculateCommission(
                stagePayment.getAmount(),
                stagePayment.getCommissionRate()
            );
            
            // Cộng net amount vào wallet
            Artisan artisan = stagePayment.getStage().getCustomOrder().getArtisan();
            WalletTransaction walletTx = walletService.addBalance(
                artisan.getWallet(),
                calc.getNetAmount(),
                "Thanh toán giai đoạn #" + stagePayment.getStage().getStageNumber()
            );
            
            // Ghi commission
            commissionService.applyCommissionToWalletTransaction(
                walletTx,
                calc.getCommissionAmount(),
                stagePayment.getCommissionRate()
            );
            
            walletTransactionRepository.save(walletTx);
            
            // Notification
            notificationService.notifyArtisanCommissionDeducted(
                artisan,
                stagePayment.getStage().getCustomOrder().getOrderId(),
                calc.getOriginalAmount(),
                calc.getCommissionAmount(),
                calc.getNetAmount()
            );
        }
    }
}
```

### 5. Controller Layer

#### CommissionController.java (Mới)

```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CommissionController {
    
    private final SystemConfigService systemConfigService;
    private final CommissionReportService commissionReportService;
    
    // Public endpoint - xem commission rate hiện tại
    @GetMapping("/commission/rate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<CommissionRateResponse>> getCurrentRate() {
        BigDecimal rate = systemConfigService.getCommissionRate();
        
        CommissionRateResponse response = CommissionRateResponse.builder()
            .commissionRate(rate)
            .description("Tỷ lệ phí sàn hiện tại")
            .build();
        
        return ResponseEntity.ok(BaseResponse.success("Lấy commission rate thành công", response));
    }
    
    // Admin endpoint - xem config
    @GetMapping("/admin/commission/config")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<CommissionConfigResponse>> getConfig() {
        SystemConfig config = systemConfigService.getConfig("PLATFORM_COMMISSION_RATE");
        
        CommissionConfigResponse response = CommissionConfigResponse.builder()
            .commissionRate(new BigDecimal(config.getConfigValue()))
            .updatedAt(config.getUpdatedAt())
            .updatedBy(config.getUpdatedBy() != null ? config.getUpdatedBy().getEmail() : null)
            .allowedUpdateTime("00:00 - 00:59")
            .build();
        
        return ResponseEntity.ok(BaseResponse.success("Lấy cấu hình thành công", response));
    }
    
    // Admin endpoint - cập nhật config
    @PutMapping("/admin/commission/config")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<CommissionConfigResponse>> updateConfig(
            @Valid @RequestBody UpdateCommissionRateRequest request,
            @AuthenticationPrincipal UUID adminId) {
        
        systemConfigService.updateCommissionRate(request.getCommissionRate(), adminId);
        
        SystemConfig config = systemConfigService.getConfig("PLATFORM_COMMISSION_RATE");
        CommissionConfigResponse response = CommissionConfigResponse.builder()
            .commissionRate(new BigDecimal(config.getConfigValue()))
            .updatedAt(config.getUpdatedAt())
            .updatedBy(config.getUpdatedBy().getEmail())
            .build();
        
        return ResponseEntity.ok(BaseResponse.success("Cập nhật commission rate thành công", response));
    }
    
    // Admin endpoint - báo cáo commission
    @GetMapping("/admin/commission/report")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponse<CommissionReportResponse>> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String groupBy) {
        
        CommissionReportResponse report = commissionReportService.generateReport(
            startDate, endDate, GroupBy.valueOf(groupBy)
        );
        
        return ResponseEntity.ok(BaseResponse.success("Tạo báo cáo thành công", report));
    }
}
```

### 6. DTOs

```java
@Data
@Builder
public class CommissionRateResponse {
    private BigDecimal commissionRate;
    private String description;
}

@Data
@Builder
public class CommissionConfigResponse {
    private BigDecimal commissionRate;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String allowedUpdateTime;
}

@Data
public class UpdateCommissionRateRequest {
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal commissionRate;
}

@Data
@Builder
public class CommissionReportResponse {
    private BigDecimal totalCommission;
    private Long totalTransactions;
    private BigDecimal averageCommissionPerTransaction;
    private List<CommissionReportItem> items;
}

@Data
@Builder
public class CommissionReportItem {
    private LocalDate date;
    private BigDecimal totalCommission;
    private Long transactionCount;
}
```

## Xử Lý Lỗi

### Exception Classes

```java
public class CommissionException extends RuntimeException {
    public CommissionException(String message) {
        super(message);
    }
}

public class InvalidTimeWindowException extends CommissionException {
    public InvalidTimeWindowException() {
        super("Chỉ có thể cập nhật phí sàn từ 00:00-00:59");
    }
}
```

## Testing Strategy

### Unit Tests

1. SystemConfigServiceTest - test cache, validation
2. CommissionServiceTest - test calculation logic
3. PaymentServiceTest - test commission integration

### Integration Tests

1. Test toàn bộ flow: create payment → callback → wallet update
2. Test time window validation
3. Test commission calculation với nhiều scenarios

## Deployment Notes

### Migration Script

```sql
-- Run in production
START TRANSACTION;

-- 1. Create system_config table
CREATE TABLE system_config (...);

-- 2. Insert default commission rate
INSERT INTO system_config VALUES (...);

-- 3. Add columns to existing tables
ALTER TABLE payments ADD COLUMN commission_rate ...;
ALTER TABLE stage_payments ADD COLUMN commission_rate ...;
ALTER TABLE wallet_transactions ADD COLUMN commission_fee ...;
ALTER TABLE wallet_transactions ADD COLUMN commission_rate ...;

-- 4. Create indexes
CREATE INDEX idx_wallet_transactions_commission ...;

COMMIT;
```

### Configuration

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    
commission:
  default-rate: 5.00
  update-time-start: 00:00
  update-time-end: 00:59
```

## Performance Considerations

1. **Redis Cache**: Commission rate được cache 24h, giảm query database
2. **Index**: Index trên commission_fee để tối ưu báo cáo
3. **Async Notification**: Gửi notification không đồng bộ để không block payment flow

## Security

1. **Time Window**: Chỉ cho phép cập nhật commission từ 00:00-00:59
2. **Admin Only**: Chỉ ADMIN có quyền cập nhật commission rate
3. **Audit Log**: Log mọi thay đổi commission rate
4. **Validation**: Validate rate trong khoảng 0-100%
