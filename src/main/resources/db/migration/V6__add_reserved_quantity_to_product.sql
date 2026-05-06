-- Add reserved_quantity column to product table
ALTER TABLE product ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER NOT NULL DEFAULT 0;
