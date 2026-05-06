package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.OrderGroup;

import java.util.UUID;

/**
 * Service for managing product inventory reservations
 */
public interface InventoryService {
    
    /**
     * Confirm reservations and deduct actual stock when payment succeeds
     */
    void confirmReservations(OrderGroup orderGroup);
    
    /**
     * Release reservations when payment fails or expires
     */
    void releaseReservations(OrderGroup orderGroup);
    
    /**
     * Release reservations for a specific order
     */
    void releaseReservationsForOrder(UUID orderId);
}
