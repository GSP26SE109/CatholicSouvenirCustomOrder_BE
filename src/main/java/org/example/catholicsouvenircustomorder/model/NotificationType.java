package org.example.catholicsouvenircustomorder.model;

public enum NotificationType {
    // Customer notifications
    ORDER_CREATED,         // When custom order is created with stages
    ORDER_CONFIRMED,       // When customer confirms order (Request-Based flow)
    STAGE_STARTED,         // When artisan starts a stage
    STAGE_COMPLETED,       // When artisan completes a stage
    ORDER_SHIPPED,         // When order is shipped
    ORDER_DELIVERED,       // When order is delivered
    ORDER_COMPLETED,       // When artisan completes custom order
    REQUEST_ACCEPTED,      // When artisan accepts custom request
    REQUEST_REJECTED,      // When artisan rejects custom request
    PAYMENT_REQUIRED,      // When stage payment is required
    
    // Artisan notifications
    NEW_CUSTOM_REQUEST,    // When customer creates custom request
    REQUEST_CONFIRMED,     // When customer confirms and selects artisan
    PAYMENT_RECEIVED,      // When payment is received
    PAYMENT_PENDING,       // When payment is pending
    
    // Chat & Conversation
    NEW_CONVERSATION,      // When conversation is created
    NEW_MESSAGE,           // When new chat message arrives
    
    // Withdrawal notifications
    WITHDRAWAL_REQUESTED,  // When artisan creates withdrawal request
    WITHDRAWAL_APPROVED,   // When admin approves withdrawal
    WITHDRAWAL_REJECTED,   // When admin rejects withdrawal
    WITHDRAWAL_CANCELLED,  // When artisan cancels withdrawal
    
    // Complaint & Refund notifications
    COMPLAINT_CREATED,     // When customer creates complaint
    ARTISAN_RESPONDED,     // When artisan responds to complaint
    COMPLAINT_APPROVED,    // When admin approves complaint
    COMPLAINT_REJECTED,    // When admin rejects complaint
    REFUND_COMPLETED,      // When refund is completed
    REFUND_FAILED,         // When refund fails
    
    // Cancellation notifications
    ORDER_CANCELLED,       // When order is cancelled by customer or artisan
    INSURANCE_FUND_USED,   // When insurance fund is used to cover refund shortage
    OFFLINE_RECOVERY_REQUIRED, // When artisan has insufficient balance for refund
    
    // Commission notifications
    COMMISSION_RATE_UPDATED, // When admin updates commission rate
    COMMISSION_DEDUCTED,     // When commission is deducted from payment
    
    // General
    SYSTEM_ANNOUNCEMENT,
    ACCOUNT_VERIFIED
}
