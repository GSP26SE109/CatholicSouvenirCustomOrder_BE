-- Fix migration for is_return column in shipments table
-- Run this SQL script in your PostgreSQL database

-- Step 1: Add column as nullable first (if not exists)
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS is_return BOOLEAN;

-- Step 2: Set default value for existing records
UPDATE shipments SET is_return = false WHERE is_return IS NULL;

-- Step 3: Add NOT NULL constraint
ALTER TABLE shipments ALTER COLUMN is_return SET NOT NULL;

-- Step 4: Set default for future records
ALTER TABLE shipments ALTER COLUMN is_return SET DEFAULT false;

-- Step 5: Add complaint_id column if not exists
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS complaint_id UUID;

-- Step 6: Add foreign key constraint for complaint_id
ALTER TABLE shipments 
ADD CONSTRAINT fk_shipment_complaint 
FOREIGN KEY (complaint_id) 
REFERENCES complaint(complaint_id) 
ON DELETE SET NULL;
