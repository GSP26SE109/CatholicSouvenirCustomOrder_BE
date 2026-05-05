-- Migration to add rejection_reason column to custom_orders table
-- This stores the reason when customer rejects an order before payment

-- Add rejection_reason column
ALTER TABLE custom_orders 
ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

-- Add comment for documentation
COMMENT ON COLUMN custom_orders.rejection_reason IS 'Reason provided by customer when rejecting order before payment (PENDING_CONFIRMATION status)';
