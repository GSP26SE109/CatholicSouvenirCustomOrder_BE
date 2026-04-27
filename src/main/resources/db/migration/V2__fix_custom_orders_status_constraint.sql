-- ============================================
-- COMPREHENSIVE STATUS CONSTRAINTS FIX
-- ============================================
-- This migration updates all status check constraints to match current enum values in code

-- 1. CUSTOM_ORDERS TABLE
-- Drop old constraint if exists
ALTER TABLE custom_orders DROP CONSTRAINT IF EXISTS custom_orders_status_check;

-- Add new constraint with all valid CustomOrderStatus values
ALTER TABLE custom_orders ADD CONSTRAINT custom_orders_status_check 
CHECK (status IN (
    'PENDING_CONFIRMATION',
    'PENDING_PAYMENT',
    'CONFIRMED',
    'IN_PROGRESS',
    'IN_PRODUCTION',
    'SHIPPING',
    'DELIVERED',
    'COMPLETED',
    'CANCELLED',
    'REFUNDED'
));

-- 2. CUSTOM_REQUESTS TABLE
ALTER TABLE custom_requests DROP CONSTRAINT IF EXISTS custom_requests_status_check;

ALTER TABLE custom_requests ADD CONSTRAINT custom_requests_status_check 
CHECK (status IN (
    'DRAFT',
    'OPEN',
    'ARTISAN_SELECTED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
));

-- 3. CUSTOM_ORDER_STAGES TABLE
ALTER TABLE custom_order_stages DROP CONSTRAINT IF EXISTS custom_order_stages_status_check;

ALTER TABLE custom_order_stages ADD CONSTRAINT custom_order_stages_status_check 
CHECK (status IN (
    'PENDING',
    'PAID',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
));

-- 4. PAYMENTS TABLE
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;

ALTER TABLE payments ADD CONSTRAINT payments_status_check 
CHECK (status IN (
    'PENDING',
    'PROCESSING',
    'SUCCESS',
    'FAILED',
    'CANCELLED',
    'REFUNDED'
));

-- 5. SHIPMENTS TABLE
ALTER TABLE shipments DROP CONSTRAINT IF EXISTS shipments_status_check;

ALTER TABLE shipments ADD CONSTRAINT shipments_status_check 
CHECK (status IN (
    'PENDING',
    'PICKING',
    'PICKED',
    'STORING',
    'TRANSPORTING',
    'DELIVERING',
    'DELIVERED',
    'RETURNED',
    'CANCELLED'
));

-- 6. REFUND_TRANSACTIONS TABLE
ALTER TABLE refund_transactions DROP CONSTRAINT IF EXISTS refund_transactions_status_check;

ALTER TABLE refund_transactions ADD CONSTRAINT refund_transactions_status_check 
CHECK (status IN (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'PARTIALLY_REFUNDED'
));

-- 7. COMPLAINTS TABLE
ALTER TABLE complaints DROP CONSTRAINT IF EXISTS complaints_status_check;

ALTER TABLE complaints ADD CONSTRAINT complaints_status_check 
CHECK (status IN (
    'PENDING',
    'WAITING_RETURN',
    'PROCESSING_REFUND',
    'APPROVED',
    'REJECTED'
));

-- 8. WITHDRAWAL_REQUESTS TABLE
ALTER TABLE withdrawal_requests DROP CONSTRAINT IF EXISTS withdrawal_requests_status_check;

ALTER TABLE withdrawal_requests ADD CONSTRAINT withdrawal_requests_status_check 
CHECK (status IN (
    'PENDING',
    'APPROVED',
    'REJECTED',
    'CANCELLED'
));

-- 9. ARTISAN_APPLICATION TABLE
ALTER TABLE artisan_application DROP CONSTRAINT IF EXISTS artisan_application_status_check;

ALTER TABLE artisan_application ADD CONSTRAINT artisan_application_status_check 
CHECK (status IN (
    'PENDING',
    'APPROVED',
    'REJECTED'
));

