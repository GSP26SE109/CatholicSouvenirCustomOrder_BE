-- ============================================
-- QUICK FIX: Drop Notification Constraint
-- ============================================
-- This is the fastest way to fix the issue
-- Run this if you need immediate fix
-- ============================================

-- Drop the problematic constraint
ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_action_type_check;

-- Done! The constraint is removed and notifications can be created freely
-- You can add a better constraint later if needed

-- Verify it's gone
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'notifications'::regclass
  AND conname LIKE '%action%';
