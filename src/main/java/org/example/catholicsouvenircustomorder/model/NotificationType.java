package org.example.catholicsouvenircustomorder.model;

public enum NotificationType {
    // Customer notifications
    NEW_QUOTATION,
    QUOTATION_UPDATED,
    ORDER_CREATED,
    STAGE_COMPLETED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    
    // Artisan notifications
    NEW_CUSTOM_REQUEST,
    REQUEST_CONFIRMED,
    PAYMENT_RECEIVED,
    PAYMENT_PENDING,
    
    // General
    SYSTEM_ANNOUNCEMENT,
    CHAT_MESSAGE,
    ACCOUNT_VERIFIED
}
