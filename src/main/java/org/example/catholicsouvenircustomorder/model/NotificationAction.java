package org.example.catholicsouvenircustomorder.model;

public enum NotificationAction {
    NONE,                    // Just informational, no action required
    
    // Custom Request Actions
    ACCEPT_REQUEST,          // Artisan accepts custom request
    REJECT_REQUEST,          // Artisan rejects custom request
    VIEW_REQUEST,            // View custom request details
    CONFIRM_ARTISAN,         // Customer confirms and selects artisan
    
    // Custom Order & Stage Actions
    VIEW_ORDER,              // View custom order details
    PAY_STAGE,               // Customer pays for custom order stage
    COMPLETE_STAGE,          // Artisan marks stage as completed
    APPROVE_STAGE,           // Customer approves completed stage
    
    // Communication Actions
    VIEW_CONVERSATION,       // View conversation/chat messages
    
    // General Actions
    VIEW_NOTIFICATION        // View notification details
}
