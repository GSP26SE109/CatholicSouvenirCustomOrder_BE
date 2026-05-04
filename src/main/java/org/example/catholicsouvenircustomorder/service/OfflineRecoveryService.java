package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.CustomOrder;

import java.math.BigDecimal;

/**
 * Service for handling offline recovery when Artisan cannot refund due to insufficient balance
 */
public interface OfflineRecoveryService {
    
    /**
     * Create offline recovery task when Artisan cannot refund
     * 
     * @param customOrder The order that requires refund
     * @param refundAmount The amount that needs to be refunded
     * @param reason The reason for the recovery task
     */
    void createRecoveryTask(CustomOrder customOrder, BigDecimal refundAmount, String reason);
}
