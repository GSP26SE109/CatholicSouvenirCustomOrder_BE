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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

/**
 * Implementation of SystemConfigService
 * Manages commission rate with Redis caching
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConfigServiceImp implements SystemConfigService {
    
    private static final String COMMISSION_RATE_KEY = "PLATFORM_COMMISSION_RATE";
    private static final String CACHE_KEY = "system:config:commission_rate";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    
    private final SystemConfigRepository systemConfigRepository;
    private final AccountRepository accountRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationService notificationService;
    
    @Override
    public BigDecimal getCommissionRate() {
        // Try cache first
        String cachedValue = stringRedisTemplate.opsForValue().get(CACHE_KEY);
        if (cachedValue != null) {
            log.debug("Commission rate from cache: {}", cachedValue);
            return new BigDecimal(cachedValue);
        }
        
        // Get from database
        SystemConfig config = systemConfigRepository.findByConfigKey(COMMISSION_RATE_KEY)
            .orElseGet(() -> createDefaultConfig());
        
        BigDecimal rate = new BigDecimal(config.getConfigValue());
        
        // Cache it
        stringRedisTemplate.opsForValue().set(CACHE_KEY, rate.toString(), CACHE_TTL);
        log.info("Commission rate loaded from database and cached: {}", rate);
        
        return rate;
    }
    
    @Override
    @Transactional
    public void updateCommissionRate(BigDecimal newRate, UUID adminId) {
        // Validate rate range (0-100)
        if (newRate.compareTo(BigDecimal.ZERO) < 0 || 
            newRate.compareTo(new BigDecimal("100")) > 0) {
            log.warn("Invalid commission rate: {}. Must be between 0 and 100", newRate);
            throw new BadRequestException("Commission rate phải từ 0 đến 100");
        }
        
        SystemConfig config = systemConfigRepository.findByConfigKey(COMMISSION_RATE_KEY)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình: " + COMMISSION_RATE_KEY));
        
        String oldValue = config.getConfigValue();
        config.setConfigValue(newRate.toString());
        
        Account admin = accountRepository.findById(adminId).orElse(null);
        config.setUpdatedBy(admin);
        
        systemConfigRepository.save(config);
        
        // Clear cache
        stringRedisTemplate.delete(CACHE_KEY);
        
        // Log change (timestamp, old value, new value, admin ID)
        log.info("Commission rate updated from {} to {} by admin {} at {}", 
            oldValue, newRate, adminId, java.time.LocalDateTime.now());
        
        // Send notification to all artisans
        notificationService.notifyAllArtisansCommissionChange(oldValue, newRate.toString());
    }
    
    @Override
    public SystemConfig getConfig(String key) {
        return systemConfigRepository.findByConfigKey(key)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình: " + key));
    }
    
    /**
     * Create default commission rate config if not exists
     */
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
