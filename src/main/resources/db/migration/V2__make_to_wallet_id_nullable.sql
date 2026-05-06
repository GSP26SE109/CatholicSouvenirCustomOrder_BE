-- Make to_wallet_id nullable in refund_transactions table
-- Customer receives refund via VNPay directly, not through platform wallet
ALTER TABLE refund_transactions ALTER COLUMN to_wallet_id DROP NOT NULL;
