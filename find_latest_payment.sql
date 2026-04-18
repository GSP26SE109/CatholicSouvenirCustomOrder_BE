-- ============================================
-- Find Latest Payment for Order Group
-- ============================================
-- Use this to find the most recent payment (not cancelled ones)
-- ============================================

-- Find all payments for this order group
SELECT 
    payment_id,
    reference_id,
    status,
    amount,
    method,
    transaction_id,
    failure_reason,
    created_at,
    paid_at,
    commission_rate
FROM payments
WHERE group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
ORDER BY created_at DESC;

-- Find the LATEST payment (not cancelled)
SELECT 
    payment_id,
    reference_id,
    status,
    amount,
    method,
    transaction_id,
    created_at,
    paid_at
FROM payments
WHERE group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
  AND status != 'CANCELLED'
ORDER BY created_at DESC
LIMIT 1;

-- Find the LATEST payment (including cancelled)
SELECT 
    payment_id,
    reference_id,
    status,
    amount,
    method,
    transaction_id,
    failure_reason,
    created_at,
    paid_at
FROM payments
WHERE group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
ORDER BY created_at DESC
LIMIT 1;

-- Check if order group is paid
SELECT 
    og.group_id,
    og.status as order_group_status,
    og.total_amount,
    COUNT(DISTINCT p.payment_id) as total_payments,
    COUNT(DISTINCT CASE WHEN p.status = 'SUCCESS' THEN p.payment_id END) as successful_payments,
    COUNT(DISTINCT CASE WHEN p.status = 'PENDING' THEN p.payment_id END) as pending_payments,
    COUNT(DISTINCT CASE WHEN p.status = 'CANCELLED' THEN p.payment_id END) as cancelled_payments
FROM order_groups og
LEFT JOIN payments p ON p.group_id = og.group_id
WHERE og.group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
GROUP BY og.group_id, og.status, og.total_amount;

-- ============================================
-- Explanation
-- ============================================
-- If you see:
-- - Multiple CANCELLED payments: User created payment multiple times
-- - One SUCCESS payment: Payment completed successfully
-- - One PENDING payment: Payment not completed yet
-- 
-- The CANCELLED payments are OLD payments that were replaced
-- You should look at the LATEST payment (highest created_at)
-- ============================================
