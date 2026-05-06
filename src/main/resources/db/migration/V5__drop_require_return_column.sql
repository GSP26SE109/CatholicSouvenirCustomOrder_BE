-- Drop require_return column from complaints table
-- Feature removed: No longer require product return before refund
ALTER TABLE complaints DROP COLUMN IF EXISTS require_return;
