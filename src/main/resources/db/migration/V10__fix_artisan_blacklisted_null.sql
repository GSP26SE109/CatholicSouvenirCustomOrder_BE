-- Fix null values in is_blacklisted column
UPDATE artisan 
SET is_blacklisted = false 
WHERE is_blacklisted IS NULL;

-- Alter column to not allow null
ALTER TABLE artisan 
ALTER COLUMN is_blacklisted SET NOT NULL;

-- Set default value
ALTER TABLE artisan 
ALTER COLUMN is_blacklisted SET DEFAULT false;
