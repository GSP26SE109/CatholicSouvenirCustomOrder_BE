package org.example.catholicsouvenircustomorder.model;

public enum NotificationAction {
    NONE,                    // Just informational
    ACCEPT_REQUEST,          // Artisan accepts custom request
    REJECT_REQUEST,          // Artisan rejects custom request
    VIEW_REQUEST,            // View custom request details
    VIEW_QUOTATION,          // Customer views quotation
    VIEW_CONVERSATION,       // View conversation/chat
    PAY_STAGE,              // Customer pays stage
    COMPLETE_STAGE,         // Artisan completes stage
    CONFIRM_ARTISAN,        // Customer confirms artisan
    VIEW_ORDER              // View order details
}
