-- Migration: Add VNPay refund fields to refund_transactions table
-- Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
-- Description: Refactor refund_transactions to support VNPay refunds instead of Customer wallet

-- Step 1: Add new VNPay refund columns
ALTER TABLE refund_transactions 
ADD COLUMN IF NOT EXISTS vnpay_refund_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS vnpay_transaction_no VARCHAR(100),
ADD COLUMN IF NOT EXISTS original_payment_id UUID;

-- Step 2: Make to_wallet_id nullable (for backward compatibility)
-- This allows existing records to remain valid while new records won't use it
-- Note: This will only execute if the column exists
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'refund_transactions' 
        AND column_name = 'to_wallet_id'
    ) THEN
        ALTER TABLE refund_transactions 
        ALTER COLUMN to_wallet_id DROP NOT NULL;
    END IF;
END $$;

-- Step 3: Make credit_transaction_id nullable (for backward compatibility)
-- This allows existing records to remain valid while new records won't use it
-- Note: This will only execute if the column exists
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'refund_transactions' 
        AND column_name = 'credit_transaction_id'
    ) THEN
        ALTER TABLE refund_transactions 
        ALTER COLUMN credit_transaction_id DROP NOT NULL;
    END IF;
END $$;

-- Step 4: Create indexes for VNPay refund lookups
CREATE INDEX IF NOT EXISTS idx_refund_vnpay_id 
ON refund_transactions(vnpay_refund_id);

CREATE INDEX IF NOT EXISTS idx_refund_original_payment 
ON refund_transactions(original_payment_id);

-- Step 5: Add comment to document the change
-- Note: Comments will only be added if columns exist
DO $$ 
BEGIN
    -- Add comments for new VNPay columns
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refund_transactions' AND column_name = 'vnpay_refund_id') THEN
        EXECUTE 'COMMENT ON COLUMN refund_transactions.vnpay_refund_id IS ''VNPay refund transaction ID returned from VNPay API''';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refund_transactions' AND column_name = 'vnpay_transaction_no') THEN
        EXECUTE 'COMMENT ON COLUMN refund_transactions.vnpay_transaction_no IS ''VNPay transaction number for the refund''';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refund_transactions' AND column_name = 'original_payment_id') THEN
        EXECUTE 'COMMENT ON COLUMN refund_transactions.original_payment_id IS ''Reference to the original Payment or StagePayment entity''';
    END IF;
    
    -- Add deprecation comments for old columns if they exist
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refund_transactions' AND column_name = 'to_wallet_id') THEN
        EXECUTE 'COMMENT ON COLUMN refund_transactions.to_wallet_id IS ''DEPRECATED: Customer wallet no longer used for refunds''';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refund_transactions' AND column_name = 'credit_transaction_id') THEN
        EXECUTE 'COMMENT ON COLUMN refund_transactions.credit_transaction_id IS ''DEPRECATED: Customer wallet transactions no longer created''';
    END IF;
END $$;
