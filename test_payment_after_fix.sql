-- ============================================
-- Test Payment After Notification Fix
-- ============================================
-- Use this to verify payment status and retry distribution
-- ============================================

-- 1. Check current payment status
SELECT 
    payment_id,
    reference_id,
    status,
    amount,
    method,
    transaction_id,
    created_at,
    paid_at,
    commission_rate
FROM payments
WHERE reference_id = 'GROUP_1d5d17b2-a674-46b0-89de-e14575ecdcf8_1776531310884';

-- 2. Check order group status
SELECT 
    og.group_id,
    og.status,
    og.total_amount,
    og.created_at,
    og.updated_at,
    COUNT(o.order_id) as order_count
FROM order_groups og
LEFT JOIN orders o ON o.group_id = og.group_id
WHERE og.group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
GROUP BY og.group_id, og.status, og.total_amount, og.created_at, og.updated_at;

-- 3. Check orders in the group
SELECT 
    o.order_id,
    o.status,
    o.total_amount,
    a.artisan_uuid,
    acc.full_name as artisan_name
FROM orders o
JOIN artisans a ON o.artisan_id = a.artisan_uuid
JOIN accounts acc ON a.account_id = acc.account_id
WHERE o.group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8';

-- 4. Check wallet transactions (to see if distribution happened)
SELECT 
    wt.transaction_id,
    wt.transaction_type,
    wt.amount,
    wt.balance_after,
    wt.description,
    wt.created_at,
    acc.full_name as account_name,
    acc.email
FROM wallet_transactions wt
JOIN wallets w ON wt.wallet_id = w.wallet_id
JOIN accounts acc ON w.account_id = acc.account_id
WHERE wt.created_at > NOW() - INTERVAL '1 hour'
ORDER BY wt.created_at DESC;

-- 5. Check notifications created for this payment
SELECT 
    n.notification_id,
    n.type,
    n.action_type,
    n.title,
    n.message,
    n.created_at,
    acc.full_name as recipient_name
FROM notifications n
JOIN accounts acc ON n.recipient_id = acc.account_id
WHERE n.created_at > NOW() - INTERVAL '1 hour'
  AND n.type IN ('PAYMENT_RECEIVED', 'COMMISSION_DEDUCTED')
ORDER BY n.created_at DESC;

-- ============================================
-- Manual Fix: Update payment to SUCCESS if still PENDING
-- ============================================
-- ONLY run this if payment is still PENDING after callback

-- Update payment status
UPDATE payments
SET 
    status = 'SUCCESS',
    transaction_id = 'MANUAL_FIX_' || EXTRACT(EPOCH FROM NOW())::TEXT,
    paid_at = NOW()
WHERE reference_id = 'GROUP_1d5d17b2-a674-46b0-89de-e14575ecdcf8_1776531310884'
  AND status = 'PENDING';

-- Update order group status
UPDATE order_groups
SET 
    status = 'PAID',
    updated_at = NOW()
WHERE group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
  AND status != 'PAID';

-- Update orders status
UPDATE orders
SET 
    status = 'PAID',
    update_at = NOW()
WHERE group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
  AND status != 'PAID';

-- Verify the updates
SELECT 'Payment Status' as check_type, status, COUNT(*) 
FROM payments 
WHERE reference_id = 'GROUP_1d5d17b2-a674-46b0-89de-e14575ecdcf8_1776531310884'
GROUP BY status

UNION ALL

SELECT 'Order Group Status', status, COUNT(*) 
FROM order_groups 
WHERE group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
GROUP BY status

UNION ALL

SELECT 'Orders Status', status, COUNT(*) 
FROM orders 
WHERE group_id = '1d5d17b2-a674-46b0-89de-e14575ecdcf8'
GROUP BY status;

-- ============================================
-- Note: Distribution can be retried via API
-- ============================================
-- After fixing the constraint and payment status,
-- you can retry distribution via admin endpoint:
-- POST /api/admin/payments/{paymentId}/retry-distribution
-- ============================================
