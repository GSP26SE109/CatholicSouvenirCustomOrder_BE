-- Run this SQL script directly in your PostgreSQL database
-- to fix the complaint_id NOT NULL constraint

-- Step 1: Make complaint_id nullable
ALTER TABLE refund_transactions 
ALTER COLUMN complaint_id DROP NOT NULL;

-- Step 2: Verify the change
SELECT 
    column_name, 
    is_nullable, 
    data_type 
FROM information_schema.columns 
WHERE table_name = 'refund_transactions' 
  AND column_name = 'complaint_id';

-- Expected result: is_nullable should be 'YES'
