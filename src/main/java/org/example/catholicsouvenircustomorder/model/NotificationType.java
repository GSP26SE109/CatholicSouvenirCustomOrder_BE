package org.example.catholicsouvenircustomorder.model;

public enum NotificationType {
    // Customer notifications
    ORDER_CREATED,         // When custom order is created with stages
    STAGE_COMPLETED,       // When artisan completes a stage
    ORDER_SHIPPED,         // When order is shipped
    ORDER_DELIVERED,       // When order is delivered
    ORDER_COMPLETED,       // When artisan completes custom order
    REQUEST_ACCEPTED,      // When artisan accepts custom request
    REQUEST_REJECTED,      // When artisan rejects custom request
    
    // Artisan notifications
    NEW_CUSTOM_REQUEST,    // When customer creates custom request
    REQUEST_CONFIRMED,     // When customer confirms and selects artisan
    PAYMENT_RECEIVED,      // When payment is received
    PAYMENT_PENDING,       // When payment is pending
    
    // Chat & Conversation
    NEW_CONVERSATION,      // When conversation is created
    NEW_MESSAGE,           // When new chat message arrives
    
    // General
    SYSTEM_ANNOUNCEMENT,
    ACCOUNT_VERIFIED
}
