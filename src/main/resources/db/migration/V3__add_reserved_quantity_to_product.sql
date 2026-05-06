-- Add reserved_quantity column to product table for inventory reservation
ALTER TABLE product ADD COLUMN IF NOT EXISTS reserved_quantity INTEGER NOT NULL DEFAULT 0;

-- Add check constraint to ensure reserved_quantity doesn't exceed quantity
ALTER TABLE product ADD CONSTRAINT check_reserved_quantity 
    CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity);

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_product_available_stock 
    ON product ((quantity - reserved_quantity));
