-- ============================================
-- Fix Notification Constraint Issue
-- ============================================
-- Problem: VIEW_WALLET_TRANSACTION action_type not allowed with COMMISSION_DEDUCTED type
-- Solution: Drop old constraint and create new one with proper rules
-- ============================================

-- Step 1: Check current constraint
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'notifications'::regclass
  AND conname = 'notifications_action_type_check';

-- Step 2: Drop the old constraint
ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_action_type_check;

-- Step 3: Create new constraint with proper rules
-- Rule: If action_type is set, it must be a valid NotificationAction value
-- Allow NULL action_type for informational notifications
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

-- Step 4: Verify the new constraint
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'notifications'::regclass
  AND conname = 'notifications_action_type_check';

-- ============================================
-- Optional: Update existing invalid notifications
-- ============================================
-- If there are existing notifications with invalid action_type, fix them:

-- Option 1: Set invalid action_type to NULL
UPDATE notifications
SET action_type = NULL
WHERE action_type NOT IN (
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
);

-- Option 2: Set commission-related notifications to proper action_type
UPDATE notifications
SET action_type = 'VIEW_WALLET_TRANSACTION'
WHERE type = 'COMMISSION_DEDUCTED'
  AND (action_type IS NULL OR action_type = 'NONE');

-- ============================================
-- Verification Queries
-- ============================================

-- Check all notifications with COMMISSION_DEDUCTED type
SELECT 
    notification_id,
    type,
    action_type,
    title,
    created_at
FROM notifications
WHERE type = 'COMMISSION_DEDUCTED'
ORDER BY created_at DESC
LIMIT 10;

-- Check for any notifications that might violate the new constraint
SELECT 
    notification_id,
    type,
    action_type,
    title
FROM notifications
WHERE action_type IS NOT NULL
  AND action_type NOT IN (
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
  );

-- ============================================
-- Summary
-- ============================================
-- This script:
-- 1. Drops the old constraint that was blocking COMMISSION_DEDUCTED notifications
-- 2. Creates a new constraint that allows all valid NotificationAction values
-- 3. Updates any existing invalid notifications
-- 4. Provides verification queries to check the results
-- ============================================
