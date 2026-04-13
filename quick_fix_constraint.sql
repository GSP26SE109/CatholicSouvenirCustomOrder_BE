-- Quick fix: Update notification action constraint to include VIEW_CONVERSATION
-- Run this immediately on your database

BEGIN;

-- Drop old constraint
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_action_type_check;

-- Add correct constraint (includes VIEW_CONVERSATION which is used in code)
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

COMMIT;

-- Verify
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'notifications'::regclass 
AND conname = 'notifications_action_type_check';
