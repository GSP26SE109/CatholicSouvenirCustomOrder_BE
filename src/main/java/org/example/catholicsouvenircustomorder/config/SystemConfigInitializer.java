package org.example.catholicsouvenircustomorder.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.model.SystemConfig;
import org.example.catholicsouvenircustomorder.repository.SystemConfigRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default system configuration values on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemConfigInitializer implements CommandLineRunner {
    
    private final SystemConfigRepository systemConfigRepository;
    
    private static final String COMMISSION_RATE_KEY = "PLATFORM_COMMISSION_RATE";
    private static final String DEFAULT_COMMISSION_RATE = "5.00";
    
    private static final String MIN_REASON_LENGTH_KEY = "CANCELLATION_MIN_REASON_LENGTH";
    private static final String DEFAULT_MIN_REASON_LENGTH = "20";
    
    private static final String LOCKED_BALANCE_PERCENTAGE_KEY = "CANCELLATION_LOCKED_BALANCE_PERCENTAGE";
    private static final String DEFAULT_LOCKED_BALANCE_PERCENTAGE = "30";
    
    private static final String LOCK_RELEASE_DAYS_KEY = "CANCELLATION_LOCK_RELEASE_DAYS";
    private static final String DEFAULT_LOCK_RELEASE_DAYS = "3";
    
    @Override
    public void run(String... args) {
        initializeCommissionRate();
        initializeCancellationSettings();
    }
    
    private void initializeCommissionRate() {
        if (!systemConfigRepository.existsById(COMMISSION_RATE_KEY)) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(COMMISSION_RATE_KEY);
            config.setConfigValue(DEFAULT_COMMISSION_RATE);
            config.setDescription("Tỷ lệ phí sàn (%) thu từ giao dịch nghệ nhân");
            
            systemConfigRepository.save(config);
            log.info("Initialized default commission rate: {}%", DEFAULT_COMMISSION_RATE);
        } else {
            log.info("Commission rate configuration already exists");
        }
    }
    
    private void initializeCancellationSettings() {
        initializeConfigIfNotExists(
            MIN_REASON_LENGTH_KEY,
            DEFAULT_MIN_REASON_LENGTH,
            "Độ dài tối thiểu của lý do hủy đơn (ký tự)"
        );
        
        initializeConfigIfNotExists(
            LOCKED_BALANCE_PERCENTAGE_KEY,
            DEFAULT_LOCKED_BALANCE_PERCENTAGE,
            "Tỷ lệ phần trăm số dư bị khóa sau khi thanh toán stage (%)"
        );
        
        initializeConfigIfNotExists(
            LOCK_RELEASE_DAYS_KEY,
            DEFAULT_LOCK_RELEASE_DAYS,
            "Số ngày sau khi hoàn thành stage để mở khóa số dư"
        );
    }
    
    private void initializeConfigIfNotExists(String key, String value, String description) {
        if (!systemConfigRepository.existsById(key)) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            
            systemConfigRepository.save(config);
            log.info("Initialized system config: {} = {}", key, value);
        } else {
            log.info("System config already exists: {}", key);
        }
    }
}
