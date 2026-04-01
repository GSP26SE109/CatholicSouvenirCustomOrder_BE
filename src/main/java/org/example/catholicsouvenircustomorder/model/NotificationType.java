package org.example.catholicsouvenircustomorder.model;

public enum NotificationType {
    // Customer notifications
    NEW_QUOTATION,
    QUOTATION_UPDATED,
    ORDER_CREATED,
    STAGE_COMPLETED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_COMPLETED,       // When artisan completes custom order
    REQUEST_ACCEPTED,      // When artisan accepts custom request
    REQUEST_REJECTED,      // When artisan rejects custom request
    
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
