package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.SystemConfig;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for managing system-wide configuration settings
 * Handles commission rate configuration with Redis caching
 */
public interface SystemConfigService {
    
    /**
     * Get current commission rate from cache or database
     * @return Current commission rate as BigDecimal
     */
    BigDecimal getCommissionRate();
    
    /**
     * Update commission rate
     * Sends notification to all artisans when updated
     * @param newRate New commission rate (0-100)
     * @param adminId Admin account ID performing the update
     * @throws BadRequestException if rate is invalid (not between 0-100)
     */
    void updateCommissionRate(BigDecimal newRate, UUID adminId);
    
    /**
     * Get system config by key
     * @param key Config key
     * @return SystemConfig entity
     * @throws ResourceNotFoundException if config not found
     */
    SystemConfig getConfig(String key);
}
