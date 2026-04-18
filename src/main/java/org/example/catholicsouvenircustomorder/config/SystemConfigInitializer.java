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
    
    @Override
    public void run(String... args) {
        initializeCommissionRate();
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
}
