-- ============================================
-- Update Notification Enums in Database
-- ============================================
-- This script updates all notification-related enum types
-- to match the current Java enum definitions
-- ============================================

-- ============================================
-- STEP 1: Drop old constraints
-- ============================================

-- Drop the problematic action_type constraint
ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_action_type_check;

-- Drop other enum constraints if they exist
ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_related_entity_type_check;

ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_priority_check;

-- ============================================
-- STEP 2: Update NotificationType enum
-- ============================================

-- Check current notification types in use
SELECT DISTINCT type, COUNT(*) as count
FROM notifications
GROUP BY type
ORDER BY type;

-- Add new NotificationType values if needed
-- PostgreSQL will automatically handle enum values
-- No need to explicitly add them if using VARCHAR

-- ============================================
-- STEP 3: Update NotificationAction enum
-- ============================================

-- Create new constraint for action_type with all valid values
ALTER TABLE notifications
ADD CONSTRAINT notifications_action_type_check CHECK (
    action_type IS NULL OR
    action_type IN (
        'NONE',
        'ACCEPT_REQUEST',
        'REJECT_REQUEST',
        'VIEW_REQUEST',
        'CONFIRM_ARTISAN',
        'VIEW_ORDER',
        'PAY_STAGE',
        'COMPLETE_STAGE',
        'APPROVE_STAGE',
        'VIEW_CONVERSATION',
        'VIEW_WALLET_TRANSACTION',
        'VIEW_NOTIFICATION'
    )
);

-- ============================================
-- STEP 4: Update NotificationType constraint
-- ============================================

ALTER TABLE notifications
ADD CONSTRAINT notifications_type_check CHECK (
    type IN (
        -- Customer notifications
        'ORDER_CREATED',
        'STAGE_COMPLETED',
        'ORDER_SHIPPED',
        'ORDER_DELIVERED',
        'ORDER_COMPLETED',
        'REQUEST_ACCEPTED',
        'REQUEST_REJECTED',
        
        -- Artisan notifications
        'NEW_CUSTOM_REQUEST',
        'REQUEST_CONFIRMED',
        'PAYMENT_RECEIVED',
        'PAYMENT_PENDING',
        
        -- Chat & Conversation
        'NEW_CONVERSATION',
        'NEW_MESSAGE',
        
        -- Withdrawal notifications
        'WITHDRAWAL_REQUESTED',
        'WITHDRAWAL_APPROVED',
        'WITHDRAWAL_REJECTED',
        'WITHDRAWAL_CANCELLED',
        
        -- Complaint & Refund notifications
        'COMPLAINT_CREATED',
        'ARTISAN_RESPONDED',
        'COMPLAINT_APPROVED',
        'COMPLAINT_REJECTED',
        'REFUND_COMPLETED',
        'REFUND_FAILED',
        
        -- Commission notifications
        'COMMISSION_RATE_UPDATED',
        'COMMISSION_DEDUCTED',
        
        -- General
        'SYSTEM_ANNOUNCEMENT',
        'ACCOUNT_VERIFIED'
    )
);

-- ============================================
-- STEP 5: Update RelatedEntityType constraint
-- ============================================

ALTER TABLE notifications
ADD CONSTRAINT notifications_related_entity_type_check CHECK (
    related_entity_type IS NULL OR
    related_entity_type IN (
        'CUSTOM_REQUEST',
        'CUSTOM_ORDER',
        'STAGE',
        'PAYMENT',
        'CONVERSATION',
        'CHAT_MESSAGE',
        'ACCOUNT',
        'WITHDRAWAL_REQUEST',
        'COMPLAINT',
        'SYSTEM_CONFIG',
        'WALLET_TRANSACTION'
    )
);

-- ============================================
-- STEP 6: Update NotificationPriority constraint
-- ============================================

ALTER TABLE notifications
ADD CONSTRAINT notifications_priority_check CHECK (
    priority IN (
        'LOW',
        'NORMAL',
        'HIGH',
        'URGENT'
    )
);

-- ============================================
-- STEP 7: Verify all constraints
-- ============================================

SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'notifications'::regclass
  AND contype = 'c'  -- Check constraints only
ORDER BY conname;

-- ============================================
-- STEP 8: Check for invalid data
-- ============================================

-- Check for invalid notification types
SELECT 
    notification_id,
    type,
    title,
    created_at
FROM notifications
WHERE type NOT IN (
    'ORDER_CREATED', 'STAGE_COMPLETED', 'ORDER_SHIPPED', 'ORDER_DELIVERED',
    'ORDER_COMPLETED', 'REQUEST_ACCEPTED', 'REQUEST_REJECTED',
    'NEW_CUSTOM_REQUEST', 'REQUEST_CONFIRMED', 'PAYMENT_RECEIVED', 'PAYMENT_PENDING',
    'NEW_CONVERSATION', 'NEW_MESSAGE',
    'WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED', 'WITHDRAWAL_CANCELLED',
    'COMPLAINT_CREATED', 'ARTISAN_RESPONDED', 'COMPLAINT_APPROVED', 'COMPLAINT_REJECTED',
    'REFUND_COMPLETED', 'REFUND_FAILED',
    'COMMISSION_RATE_UPDATED', 'COMMISSION_DEDUCTED',
    'SYSTEM_ANNOUNCEMENT', 'ACCOUNT_VERIFIED'
);

-- Check for invalid action types
SELECT 
    notification_id,
    action_type,
    type,
    title,
    created_at
FROM notifications
WHERE action_type IS NOT NULL
  AND action_type NOT IN (
    'NONE', 'ACCEPT_REQUEST', 'REJECT_REQUEST', 'VIEW_REQUEST', 'CONFIRM_ARTISAN',
    'VIEW_ORDER', 'PAY_STAGE', 'COMPLETE_STAGE', 'APPROVE_STAGE',
    'VIEW_CONVERSATION', 'VIEW_WALLET_TRANSACTION', 'VIEW_NOTIFICATION'
  );

-- Check for invalid related entity types
SELECT 
    notification_id,
    related_entity_type,
    type,
    title,
    created_at
FROM notifications
WHERE related_entity_type IS NOT NULL
  AND related_entity_type NOT IN (
    'CUSTOM_REQUEST', 'CUSTOM_ORDER', 'STAGE', 'PAYMENT',
    'CONVERSATION', 'CHAT_MESSAGE', 'ACCOUNT', 'WITHDRAWAL_REQUEST',
    'COMPLAINT', 'SYSTEM_CONFIG', 'WALLET_TRANSACTION'
  );

-- Check for invalid priorities
SELECT 
    notification_id,
    priority,
    type,
    title,
    created_at
FROM notifications
WHERE priority NOT IN ('LOW', 'NORMAL', 'HIGH', 'URGENT');

-- ============================================
-- STEP 9: Fix invalid data (if any)
-- ============================================

-- Set invalid action_type to NULL
UPDATE notifications
SET action_type = NULL
WHERE action_type IS NOT NULL
  AND action_type NOT IN (
    'NONE', 'ACCEPT_REQUEST', 'REJECT_REQUEST', 'VIEW_REQUEST', 'CONFIRM_ARTISAN',
    'VIEW_ORDER', 'PAY_STAGE', 'COMPLETE_STAGE', 'APPROVE_STAGE',
    'VIEW_CONVERSATION', 'VIEW_WALLET_TRANSACTION', 'VIEW_NOTIFICATION'
  );

-- Set invalid priority to NORMAL
UPDATE notifications
SET priority = 'NORMAL'
WHERE priority NOT IN ('LOW', 'NORMAL', 'HIGH', 'URGENT');

-- ============================================
-- STEP 10: Summary
-- ============================================

SELECT 
    'Total Notifications' as metric,
    COUNT(*) as count
FROM notifications

UNION ALL

SELECT 
    'By Type',
    COUNT(DISTINCT type)
FROM notifications

UNION ALL

SELECT 
    'By Action Type',
    COUNT(DISTINCT action_type)
FROM notifications
WHERE action_type IS NOT NULL

UNION ALL

SELECT 
    'By Related Entity Type',
    COUNT(DISTINCT related_entity_type)
FROM notifications
WHERE related_entity_type IS NOT NULL

UNION ALL

SELECT 
    'By Priority',
    COUNT(DISTINCT priority)
FROM notifications;

-- ============================================
-- COMPLETE!
-- ============================================
-- All notification enum constraints have been updated
-- to match the current Java enum definitions
-- ============================================
