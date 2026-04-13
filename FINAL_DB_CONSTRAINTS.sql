-- ============================================
-- FINAL DATABASE CONSTRAINTS - CHỐT ĐỂ DEPLOY
-- ============================================
-- Chạy script này một lần duy nhất trên production database
-- Sau khi chạy xong, restart Spring Boot application

BEGIN;

-- ============================================
-- 1. NotificationAction (action_type)
-- ============================================
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_action_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_action_type_check 
CHECK (action_type IN (
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
    'VIEW_NOTIFICATION'
));

-- ============================================
-- 2. NotificationType (type)
-- ============================================
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check 
CHECK (type IN (
    'ORDER_CREATED',
    'STAGE_COMPLETED',
    'ORDER_SHIPPED',
    'ORDER_DELIVERED',
    'ORDER_COMPLETED',
    'REQUEST_ACCEPTED',
    'REQUEST_REJECTED',
    'NEW_CUSTOM_REQUEST',
    'REQUEST_CONFIRMED',
    'PAYMENT_RECEIVED',
    'PAYMENT_PENDING',
    'NEW_CONVERSATION',
    'NEW_MESSAGE',
    'SYSTEM_ANNOUNCEMENT',
    'ACCOUNT_VERIFIED'
));

-- ============================================
-- 3. RelatedEntityType (related_entity_type)
-- ============================================
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_related_entity_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_related_entity_type_check 
CHECK (related_entity_type IN (
    'CUSTOM_REQUEST',
    'CUSTOM_ORDER',
    'STAGE',
    'PAYMENT',
    'CONVERSATION',
    'CHAT_MESSAGE',
    'ACCOUNT'
));

-- ============================================
-- 4. NotificationPriority (priority)
-- ============================================
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_priority_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_priority_check 
CHECK (priority IN (
    'LOW',
    'NORMAL',
    'HIGH',
    'URGENT'
));

-- ============================================
-- 5. Data Migration (if needed)
-- ============================================
-- Update old values to new values (if any exist)

-- Convert STAGE_PAYMENT to STAGE (if exists)
UPDATE notifications 
SET related_entity_type = 'STAGE' 
WHERE related_entity_type = 'STAGE_PAYMENT';

-- Convert VIEW_QUOTATION to VIEW_REQUEST (if exists from old flow)
UPDATE notifications 
SET action_type = 'VIEW_REQUEST' 
WHERE action_type = 'VIEW_QUOTATION';

-- Convert QUOTATION entity type to CUSTOM_REQUEST (if exists from old flow)
UPDATE notifications 
SET related_entity_type = 'CUSTOM_REQUEST' 
WHERE related_entity_type = 'QUOTATION';

COMMIT;

-- ============================================
-- Verification Queries
-- ============================================
-- Run these after migration to verify

-- Check all action types
-- SELECT action_type, COUNT(*) FROM notifications GROUP BY action_type ORDER BY action_type;

-- Check all notification types
-- SELECT type, COUNT(*) FROM notifications GROUP BY type ORDER BY type;

-- Check all entity types
-- SELECT related_entity_type, COUNT(*) FROM notifications GROUP BY related_entity_type ORDER BY related_entity_type;

-- Check all priorities
-- SELECT priority, COUNT(*) FROM notifications GROUP BY priority ORDER BY priority;

-- Check for any invalid values (should return 0 rows)
-- SELECT * FROM notifications WHERE action_type NOT IN ('NONE', 'ACCEPT_REQUEST', 'REJECT_REQUEST', 'VIEW_REQUEST', 'CONFIRM_ARTISAN', 'VIEW_ORDER', 'PAY_STAGE', 'COMPLETE_STAGE', 'APPROVE_STAGE', 'VIEW_CONVERSATION', 'VIEW_NOTIFICATION');
