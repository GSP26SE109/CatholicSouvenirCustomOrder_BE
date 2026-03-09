-- Drop artisan_notifications table
-- This table is replaced by universal notifications table

DROP TABLE IF EXISTS artisan_notifications CASCADE;

-- Add comment
COMMENT ON TABLE notifications IS 'Universal notification system - replaces artisan_notifications';
