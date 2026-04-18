-- ============================================
-- Drop All Notification Constraints (Quick Fix)
-- ============================================
-- Use this for immediate fix without validation
-- Recommended for development/testing environments
-- ============================================

-- Drop all check constraints on notifications table
ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_action_type_check;

ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_related_entity_type_check;

ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_priority_check;

-- Verify all constraints are dropped
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'notifications'::regclass
  AND contype = 'c'  -- Check constraints only
ORDER BY conname;

-- If the result is empty, all check constraints have been dropped successfully
-- ============================================
-- DONE! Notifications can now use any enum values
-- ============================================
