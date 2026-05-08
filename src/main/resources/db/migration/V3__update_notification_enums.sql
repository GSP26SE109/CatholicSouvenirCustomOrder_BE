-- Migration to ensure notification table supports all enum values
-- This script updates the type and action_type columns to support all enum values

-- Update notification type column to support all NotificationType enum values
-- PostgreSQL will automatically handle enum values if using VARCHAR
-- If using actual ENUM type, we need to add new values

-- Check if notification table exists and update constraints if needed
DO $$
BEGIN
    -- Ensure type column can store all NotificationType values
    -- If using VARCHAR, no action needed
    -- If using CHECK constraint, update it
    
    -- Drop old constraint if exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'notification_type_check' 
        AND table_name = 'notification'
    ) THEN
        ALTER TABLE notification DROP CONSTRAINT notification_type_check;
    END IF;
    
    -- Add updated constraint for NotificationType
    ALTER TABLE notification ADD CONSTRAINT notification_type_check CHECK (
        type IN (
            'ORDER_CREATED',
            'ORDER_CONFIRMED',
            'STAGE_STARTED',
            'STAGE_COMPLETED',
            'ORDER_SHIPPED',
            'ORDER_DELIVERED',
            'ORDER_COMPLETED',
            'REQUEST_ACCEPTED',
            'REQUEST_REJECTED',
            'PAYMENT_REQUIRED',
            'NEW_CUSTOM_REQUEST',
            'REQUEST_CONFIRMED',
            'PAYMENT_RECEIVED',
            'PAYMENT_PENDING',
            'NEW_CONVERSATION',
            'NEW_MESSAGE',
            'WITHDRAWAL_REQUESTED',
            'WITHDRAWAL_APPROVED',
            'WITHDRAWAL_REJECTED',
            'WITHDRAWAL_CANCELLED',
            'COMPLAINT_CREATED',
            'ARTISAN_RESPONDED',
            'COMPLAINT_APPROVED',
            'COMPLAINT_REJECTED',
            'REFUND_COMPLETED',
            'REFUND_FAILED',
            'ORDER_CANCELLED',
            'INSURANCE_FUND_USED',
            'OFFLINE_RECOVERY_REQUIRED',
            'COMMISSION_RATE_UPDATED',
            'COMMISSION_DEDUCTED',
            'SYSTEM_ANNOUNCEMENT',
            'ACCOUNT_VERIFIED'
        )
    );
    
    -- Drop old constraint for action_type if exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'notification_action_type_check' 
        AND table_name = 'notification'
    ) THEN
        ALTER TABLE notification DROP CONSTRAINT notification_action_type_check;
    END IF;
    
    -- Add updated constraint for NotificationAction
    ALTER TABLE notification ADD CONSTRAINT notification_action_type_check CHECK (
        action_type IN (
            'NONE',
            'ACCEPT_REQUEST',
            'REJECT_REQUEST',
            'VIEW_REQUEST',
            'CONFIRM_ARTISAN',
            'VIEW_ORDER',
            'PAY_STAGE',
            'COMPLETE_STAGE',
            'APPROVE_STAGE',
            'VIEW_CONVERSATION',
            'VIEW_WALLET_TRANSACTION',
            'REVIEW_RECOVERY',
            'VIEW_NOTIFICATION'
        )
    );
    
    RAISE NOTICE 'Notification enum constraints updated successfully';
    
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Error updating notification constraints: %', SQLERRM;
END $$;

-- Add indexes for better query performance on notification queries
CREATE INDEX IF NOT EXISTS idx_notification_action_type 
    ON notification(action_type) 
    WHERE action_type IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notification_action_completed 
    ON notification(action_completed) 
    WHERE action_required = true;

CREATE INDEX IF NOT EXISTS idx_notification_recipient_unread 
    ON notification(recipient_id, is_read, created_at DESC) 
    WHERE is_read = false;

-- Add comment for documentation
COMMENT ON COLUMN notification.type IS 'NotificationType enum: ORDER_CREATED, ORDER_CONFIRMED, STAGE_STARTED, STAGE_COMPLETED, ORDER_SHIPPED, ORDER_DELIVERED, ORDER_COMPLETED, REQUEST_ACCEPTED, REQUEST_REJECTED, PAYMENT_REQUIRED, NEW_CUSTOM_REQUEST, REQUEST_CONFIRMED, PAYMENT_RECEIVED, PAYMENT_PENDING, NEW_CONVERSATION, NEW_MESSAGE, WITHDRAWAL_REQUESTED, WITHDRAWAL_APPROVED, WITHDRAWAL_REJECTED, WITHDRAWAL_CANCELLED, COMPLAINT_CREATED, ARTISAN_RESPONDED, COMPLAINT_APPROVED, COMPLAINT_REJECTED, REFUND_COMPLETED, REFUND_FAILED, ORDER_CANCELLED, INSURANCE_FUND_USED, OFFLINE_RECOVERY_REQUIRED, COMMISSION_RATE_UPDATED, COMMISSION_DEDUCTED, SYSTEM_ANNOUNCEMENT, ACCOUNT_VERIFIED';

COMMENT ON COLUMN notification.action_type IS 'NotificationAction enum: NONE, ACCEPT_REQUEST, REJECT_REQUEST, VIEW_REQUEST, CONFIRM_ARTISAN, VIEW_ORDER, PAY_STAGE, COMPLETE_STAGE, APPROVE_STAGE, VIEW_CONVERSATION, VIEW_WALLET_TRANSACTION, REVIEW_RECOVERY, VIEW_NOTIFICATION';
