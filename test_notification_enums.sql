-- ============================================
-- Test Notification Enums After Update
-- ============================================
-- Use this to verify enum values are working correctly
-- ============================================

-- ============================================
-- Test 1: Check all enum values in use
-- ============================================

-- NotificationType distribution
SELECT 
    'NotificationType' as enum_type,
    type as enum_value,
    COUNT(*) as count
FROM notifications
GROUP BY type
ORDER BY count DESC;

-- NotificationAction distribution
SELECT 
    'NotificationAction' as enum_type,
    action_type as enum_value,
    COUNT(*) as count
FROM notifications
WHERE action_type IS NOT NULL
GROUP BY action_type
ORDER BY count DESC;

-- RelatedEntityType distribution
SELECT 
    'RelatedEntityType' as enum_type,
    related_entity_type as enum_value,
    COUNT(*) as count
FROM notifications
WHERE related_entity_type IS NOT NULL
GROUP BY related_entity_type
ORDER BY count DESC;

-- NotificationPriority distribution
SELECT 
    'NotificationPriority' as enum_type,
    priority as enum_value,
    COUNT(*) as count
FROM notifications
GROUP BY priority
ORDER BY count DESC;

-- ============================================
-- Test 2: Test COMMISSION_DEDUCTED notifications
-- ============================================

-- Check existing COMMISSION_DEDUCTED notifications
SELECT 
    notification_id,
    type,
    action_type,
    related_entity_type,
    priority,
    title,
    LEFT(message, 100) as message_preview,
    created_at
FROM notifications
WHERE type = 'COMMISSION_DEDUCTED'
ORDER BY created_at DESC
LIMIT 10;

-- ============================================
-- Test 3: Try to insert a test notification
-- ============================================

-- Insert a test COMMISSION_DEDUCTED notification
-- This should work after fixing the constraints
DO $$
DECLARE
    test_recipient_id UUID;
    test_wallet_tx_id UUID;
BEGIN
    -- Get a random account for testing
    SELECT account_id INTO test_recipient_id
    FROM accounts
    WHERE role = 'ARTISAN'
    LIMIT 1;
    
    -- Get a random wallet transaction for testing
    SELECT transaction_id INTO test_wallet_tx_id
    FROM wallet_transactions
    LIMIT 1;
    
    -- Try to insert test notification
    IF test_recipient_id IS NOT NULL THEN
        INSERT INTO notifications (
            recipient_id,
            type,
            title,
            message,
            action_type,
            action_required,
            action_completed,
            related_entity_id,
            related_entity_type,
            priority,
            is_read,
            metadata
        ) VALUES (
            test_recipient_id,
            'COMMISSION_DEDUCTED',
            'TEST: Phí sàn đã được trừ',
            'Đây là notification test. Số tiền gốc: 1,000,000 VND, Phí sàn (5%): 50,000 VND, Số tiền nhận: 950,000 VND',
            'VIEW_WALLET_TRANSACTION',
            false,
            false,
            test_wallet_tx_id,
            'WALLET_TRANSACTION',
            'NORMAL',
            false,
            '{"test": true, "originalAmount": 1000000, "commissionAmount": 50000, "netAmount": 950000}'
        );
        
        RAISE NOTICE 'Test notification inserted successfully!';
    ELSE
        RAISE NOTICE 'No artisan account found for testing';
    END IF;
END $$;

-- Verify the test notification was inserted
SELECT 
    notification_id,
    type,
    action_type,
    related_entity_type,
    title,
    created_at
FROM notifications
WHERE title LIKE 'TEST:%'
ORDER BY created_at DESC
LIMIT 1;

-- ============================================
-- Test 4: Clean up test data
-- ============================================

-- Delete test notifications
DELETE FROM notifications
WHERE title LIKE 'TEST:%';

-- ============================================
-- Test 5: Verify all constraints
-- ============================================

-- List all active constraints on notifications table
SELECT 
    conname AS constraint_name,
    contype AS constraint_type,
    CASE contype
        WHEN 'c' THEN 'CHECK'
        WHEN 'f' THEN 'FOREIGN KEY'
        WHEN 'p' THEN 'PRIMARY KEY'
        WHEN 'u' THEN 'UNIQUE'
        ELSE contype::text
    END AS constraint_type_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'notifications'::regclass
ORDER BY contype, conname;

-- ============================================
-- Summary
-- ============================================

SELECT 
    'Summary' as section,
    'Total Notifications' as metric,
    COUNT(*)::text as value
FROM notifications

UNION ALL

SELECT 
    'Summary',
    'Commission Notifications',
    COUNT(*)::text
FROM notifications
WHERE type = 'COMMISSION_DEDUCTED'

UNION ALL

SELECT 
    'Summary',
    'With Action Type',
    COUNT(*)::text
FROM notifications
WHERE action_type IS NOT NULL

UNION ALL

SELECT 
    'Summary',
    'With Related Entity',
    COUNT(*)::text
FROM notifications
WHERE related_entity_id IS NOT NULL

UNION ALL

SELECT 
    'Summary',
    'Unread',
    COUNT(*)::text
FROM notifications
WHERE is_read = false;

-- ============================================
-- COMPLETE!
-- ============================================
-- If all tests pass, notification enums are working correctly
-- ============================================
